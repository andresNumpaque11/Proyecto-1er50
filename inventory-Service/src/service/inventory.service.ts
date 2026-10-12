import { Types } from "mongoose";
import { Medication } from "../models/Medication";
import { InventoryLot } from "../models/InventoryLot";
import { MedicationRequest } from "../models/MedicationRequest";

interface createMedicationInput {
  name: string;
  activeIngredient: string;
  presentation: string;
  concentration: string;
}
interface CreateLotInput {
  medication: string;
  lotCode: string;
  quantity: number;
  minimumStock: number;
  expirationDate: string;
}
interface CreateMedicationRequestInput {
  medication: string;
  quantity: number;
  requestedBy: string;
}

export class InventoryServiceError extends Error {
  constructor(
    message: string,
    public readonly statusCode: number,
  ) {
    super(message);
    this.name = "InventoryServiceError";
  }
}

export async function createMedication(input: createMedicationInput) {
    
  return Medication.create(input);
}

export async function getMedications() {
  return Medication.find({ active: true }).sort({ name: 1 });
}

export async function createLot(input: CreateLotInput) {
  if (!Types.ObjectId.isValid(input.medication)) {
    throw new Error("El id del medicamento no es valido");
  }

  const medication = await Medication.findById(input.medication);

  if (!medication || !medication.active) {
    throw new Error("El medicamento no existe o esta inactivo");
  }

  const expirationDate = new Date(input.expirationDate);

  if (
    Number.isNaN(expirationDate.getTime()) ||
    !/^\d{4}-\d{2}-\d{2}$/.test(input.expirationDate) ||
    expirationDate.toISOString().slice(0, 10) !== input.expirationDate
  ) {
    throw new Error("La fecha de vencimiento no es válida");
  }

  if (expirationDate <= new Date()) {
    throw new Error("El lote debe tener una fecha de vencimiento futura");
  }

  if (!Number.isInteger(input.quantity) || input.quantity < 0) {
    throw new Error("La cantidad debe ser un entero no negativo");
  }

  if (
    input.minimumStock !== undefined &&
    (!Number.isInteger(input.minimumStock) || input.minimumStock < 0)
  ) {
    throw new Error("El stock mínimo debe ser un entero no negativo");
  }

  return InventoryLot.create({
    medication: medication._id,
    lotCode: input.lotCode,
    quantity: input.quantity,
    minimumStock: input.minimumStock ?? 10,
    expirationDate,
  });
}

export async function getLots() {
  return InventoryLot.find().populate("medication").sort({ expirationDate: 1 });
}

export async function createMedicationRequest(
  input: CreateMedicationRequestInput,
) {
  if (!Types.ObjectId.isValid(input.medication)) {
    throw new InventoryServiceError("El id del medicamento no es válido", 400);
  }

  if (!Number.isInteger(input.quantity) || input.quantity < 1) {
    throw new InventoryServiceError(
      "La cantidad solicitada debe ser un entero positivo",
      400,
    );
  }

  if (typeof input.requestedBy !== "string" || !input.requestedBy.trim()) {
    throw new InventoryServiceError(
      "El solicitante es obligatorio",
      400,
    );
  }

  const medication = await Medication.findOne({
    _id: input.medication,
    active: true,
  });
  if (!medication) {
    throw new InventoryServiceError(
      "El medicamento no existe o está inactivo",
      404,
    );
  }

  return MedicationRequest.create({
    medication: medication._id,
    quantity: input.quantity,
    requestedBy: input.requestedBy.trim(),
  });
}

export async function dispatchMedicationRequest(id: string) {
  if (!Types.ObjectId.isValid(id)) {
    throw new InventoryServiceError(
      "El id de la solicitud no es válido",
      400,
    );
  }

  const request = await MedicationRequest.findOneAndUpdate(
    { _id: id, status: "pending" },
    { $set: { status: "dispatching" } },
    { new: true },
  );

  if (!request) {
    const existingRequest = await MedicationRequest.findById(id);
    if (!existingRequest) {
      throw new InventoryServiceError("La solicitud no existe", 404);
    }
    throw new InventoryServiceError(
      existingRequest.status === "dispatched"
        ? "La solicitud ya fue despachada"
        : "La solicitud ya se está procesando",
      409,
    );
  }

  const dispatchedLots: Array<{ lot: Types.ObjectId; quantity: number }> = [];
  let dispatchCompleted = false;
  try {
    const lots = await InventoryLot.find({
      medication: request.medication,
      quantity: { $gt: 0 },
      expirationDate: { $gt: new Date() },
    }).sort({ expirationDate: 1, _id: 1 });

    const availableQuantity = lots.reduce(
      (total, lot) => total + lot.quantity,
      0,
    );
    if (availableQuantity < request.quantity) {
      throw new InventoryServiceError(
        "No hay stock suficiente para despachar la solicitud",
        409,
      );
    }

    let remainingQuantity = request.quantity;
    for (const lot of lots) {
      if (remainingQuantity === 0) {
        break;
      }

      const quantity = Math.min(lot.quantity, remainingQuantity);
      const updatedLot = await InventoryLot.updateOne(
        { _id: lot._id, quantity: { $gte: quantity } },
        { $inc: { quantity: -quantity } },
      );
      if (updatedLot.modifiedCount !== 1) {
        throw new InventoryServiceError(
          "El stock cambió durante el despacho; inténtalo nuevamente",
          409,
        );
      }

      dispatchedLots.push({ lot: lot._id, quantity });
      remainingQuantity -= quantity;
    }

    const dispatchedRequest = await MedicationRequest.findOneAndUpdate(
      { _id: request._id, status: "dispatching" },
      {
        $set: {
          status: "dispatched",
          dispatchedAt: new Date(),
          dispatchedLots,
        },
      },
      { new: true },
    );
    if (!dispatchedRequest) {
      throw new InventoryServiceError(
        "No se pudo actualizar el estado de la solicitud",
        409,
      );
    }
    dispatchCompleted = true;
  } catch (error) {
    if (!dispatchCompleted) {
      try {
        for (const dispatchedLot of dispatchedLots) {
          await InventoryLot.updateOne(
            { _id: dispatchedLot.lot },
            { $inc: { quantity: dispatchedLot.quantity } },
          );
        }
      } catch {
        throw new InventoryServiceError(
          "Falló el despacho y no se pudo revertir todo el stock; la solicitud requiere revisión",
          500,
        );
      }

      const resetRequest = await MedicationRequest.updateOne(
        { _id: request._id, status: "dispatching" },
        { $set: { status: "pending" } },
      );
      if (resetRequest.modifiedCount !== 1) {
        throw new InventoryServiceError(
          "El stock fue revertido, pero no se pudo restablecer el estado de la solicitud",
          500,
        );
      }
    }
    throw error;
  }

  return MedicationRequest.findById(request._id)
    .populate("medication")
    .populate("dispatchedLots.lot");
}

export async function adjustLotStock(id: string, adjustment: unknown) {
  if (!Types.ObjectId.isValid(id)) {
    throw new InventoryServiceError("El id del lote no es válido", 400);
  }

  if (
    typeof adjustment !== "number" ||
    !Number.isInteger(adjustment) ||
    adjustment === 0
  ) {
    throw new InventoryServiceError(
      "El ajuste de stock debe ser un entero distinto de cero",
      400,
    );
  }

  const filter: Record<string, unknown> = { _id: id };
  if (adjustment < 0) {
    filter.quantity = { $gte: Math.abs(adjustment) };
  }

  const lot = await InventoryLot.findOneAndUpdate(
    filter,
    { $inc: { quantity: adjustment } },
    { new: true, runValidators: true },
  ).populate("medication");

  if (lot) {
    return lot;
  }

  const existingLot = await InventoryLot.exists({ _id: id });
  if (!existingLot) {
    throw new InventoryServiceError("El lote no existe", 404);
  }

  throw new InventoryServiceError(
    "El ajuste dejaría el stock por debajo de cero",
    409,
  );
}

export async function getLowStockLots() {
  return InventoryLot.find({
    $expr: { $lte: ["$quantity", "$minimumStock"] },
  })
    .populate("medication")
    .sort({ quantity: 1, expirationDate: 1 });
}

export async function getExpiringLots(days: number) {
  if (!Number.isInteger(days) || days < 1) {
    throw new InventoryServiceError(
      "El número de días debe ser un entero positivo",
      400,
    );
  }

  const now = new Date();
  const cutoff = new Date(now);
  cutoff.setDate(cutoff.getDate() + days);

  return InventoryLot.find({
    expirationDate: { $gte: now, $lte: cutoff },
  })
    .populate("medication")
    .sort({ expirationDate: 1 });
}
