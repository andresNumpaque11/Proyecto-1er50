import { Types } from "mongoose";
import {
  InventoryDomainError,
  type LotInput,
  type MedicationInput,
  type MedicationRequestInput,
} from "../domain/inventory";
import type { InventoryRepositoryPort } from "./ports";

export class InventoryUseCases {
  constructor(private readonly repository: InventoryRepositoryPort) {}

  createMedication(input: MedicationInput) {
    return this.repository.createMedication(input);
  }

  getMedications() {
    return this.repository.getMedications();
  }

  async createLot(input: LotInput) {
    if (!Types.ObjectId.isValid(input.medication)) {
      throw new InventoryDomainError("El id del medicamento no es valido", 400);
    }

    const medication = await this.repository.findMedicationById(input.medication);
    if (!medication || !medication.active) {
      throw new InventoryDomainError("El medicamento no existe o esta inactivo", 400);
    }

    const expirationDate = new Date(input.expirationDate);
    if (
      Number.isNaN(expirationDate.getTime()) ||
      !/^\d{4}-\d{2}-\d{2}$/.test(input.expirationDate) ||
      expirationDate.toISOString().slice(0, 10) !== input.expirationDate
    ) {
      throw new InventoryDomainError("La fecha de vencimiento no es válida", 400);
    }
    if (expirationDate <= new Date()) {
      throw new InventoryDomainError("El lote debe tener una fecha de vencimiento futura", 400);
    }
    if (!Number.isInteger(input.quantity) || input.quantity < 0) {
      throw new InventoryDomainError("La cantidad debe ser un entero no negativo", 400);
    }
    if (!Number.isInteger(input.minimumStock) || input.minimumStock < 0) {
      throw new InventoryDomainError("El stock mínimo debe ser un entero no negativo", 400);
    }

    return this.repository.createLot({
      medication: medication._id,
      lotCode: input.lotCode,
      quantity: input.quantity,
      minimumStock: input.minimumStock,
      expirationDate,
    });
  }

  getLots() {
    return this.repository.getLots();
  }

  async createMedicationRequest(input: MedicationRequestInput) {
    if (!Types.ObjectId.isValid(input.medication)) {
      throw new InventoryDomainError("El id del medicamento no es válido", 400);
    }
    if (!Number.isInteger(input.quantity) || input.quantity < 1) {
      throw new InventoryDomainError("La cantidad solicitada debe ser un entero positivo", 400);
    }
    if (typeof input.requestedBy !== "string" || !input.requestedBy.trim()) {
      throw new InventoryDomainError("El solicitante es obligatorio", 400);
    }

    const medication = await this.repository.findActiveMedication(input.medication);
    if (!medication) {
      throw new InventoryDomainError("El medicamento no existe o está inactivo", 404);
    }
    return this.repository.createMedicationRequest({
      medication: medication._id,
      quantity: input.quantity,
      requestedBy: input.requestedBy.trim(),
    });
  }

  async dispatchMedicationRequest(id: string) {
    if (!Types.ObjectId.isValid(id)) {
      throw new InventoryDomainError("El id de la solicitud no es válido", 400);
    }

    const request = await this.repository.beginMedicationRequestDispatch(id) as {
      _id: unknown;
      medication: unknown;
      quantity: number;
    } | null;

    if (!request) {
      const existing = await this.repository.findMedicationRequestById(id) as { status?: string } | null;
      if (!existing) {
        throw new InventoryDomainError("La solicitud no existe", 404);
      }
      throw new InventoryDomainError(
        existing.status === "dispatched" ? "La solicitud ya fue despachada" : "La solicitud ya se está procesando",
        409,
      );
    }

    const dispatchedLots: Array<{ lot: unknown; quantity: number }> = [];
    let completed = false;
    try {
      const lots = await this.repository.findAvailableLots(request.medication) as Array<{
        _id: unknown;
        quantity: number;
      }>;
      const available = lots.reduce((total, lot) => total + lot.quantity, 0);
      if (available < request.quantity) {
        throw new InventoryDomainError("No hay stock suficiente para despachar la solicitud", 409);
      }

      let remaining = request.quantity;
      for (const lot of lots) {
        if (remaining === 0) break;
        const quantity = Math.min(lot.quantity, remaining);
        if (!await this.repository.decrementLot(lot._id, quantity)) {
          throw new InventoryDomainError("El stock cambió durante el despacho; inténtalo nuevamente", 409);
        }
        dispatchedLots.push({ lot: lot._id, quantity });
        remaining -= quantity;
      }

      await this.repository.completeMedicationRequest(request._id, dispatchedLots);
      completed = true;
    } catch (error) {
      if (!completed) {
        try {
          for (const dispatched of dispatchedLots) {
            await this.repository.restoreLot(dispatched.lot, dispatched.quantity);
          }
        } catch {
          throw new InventoryDomainError("Falló el despacho y no se pudo revertir todo el stock; la solicitud requiere revisión", 500);
        }
        if (!await this.repository.resetMedicationRequest(request._id)) {
          throw new InventoryDomainError("El stock fue revertido, pero no se pudo restablecer el estado de la solicitud", 500);
        }
      }
      throw error;
    }

    return this.repository.findMedicationRequestWithDetails(request._id);
  }

  async adjustLotStock(id: string, adjustment: unknown) {
    if (!Types.ObjectId.isValid(id)) {
      throw new InventoryDomainError("El id del lote no es válido", 400);
    }
    if (typeof adjustment !== "number" || !Number.isInteger(adjustment) || adjustment === 0) {
      throw new InventoryDomainError("El ajuste de stock debe ser un entero distinto de cero", 400);
    }

    const result = await this.repository.adjustLotStock(id, adjustment);
    if (result.found) return result.found;
    if (!await this.repository.lotExists(id)) {
      throw new InventoryDomainError("El lote no existe", 404);
    }
    throw new InventoryDomainError("El ajuste dejaría el stock por debajo de cero", 409);
  }

  getLowStockLots() {
    return this.repository.getLowStockLots();
  }

  getExpiringLots(days: number) {
    if (!Number.isInteger(days) || days < 1) {
      throw new InventoryDomainError("El número de días debe ser un entero positivo", 400);
    }
    const now = new Date();
    const cutoff = new Date(now);
    cutoff.setDate(cutoff.getDate() + days);
    return this.repository.getExpiringLots(now, cutoff);
  }
}
