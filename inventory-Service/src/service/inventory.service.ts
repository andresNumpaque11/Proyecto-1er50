import { Types } from "mongoose";
import { Medication } from "../models/Medication";
import { InventoryLot } from "../models/InventoryLot";

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
