import { Medication } from "../../models/Medication";
import { InventoryLot } from "../../models/InventoryLot";
import { MedicationRequest } from "../../models/MedicationRequest";
import type { InventoryRepositoryPort } from "../../application/ports";
import { InventoryDomainError } from "../../domain/inventory";
import type { Types } from "mongoose";

export class MongooseInventoryRepository implements InventoryRepositoryPort {
  createMedication(input: unknown) { return Medication.create(input as object); }
  getMedications() { return Medication.find({ active: true }).sort({ name: 1 }); }
  findMedicationById(id: string) { return Medication.findById(id); }
  findActiveMedication(id: string) { return Medication.findOne({ _id: id, active: true }); }
  createLot(input: unknown) { return InventoryLot.create(input as object); }
  getLots() { return InventoryLot.find().populate("medication").sort({ expirationDate: 1 }); }
  createMedicationRequest(input: unknown) { return MedicationRequest.create(input as object); }

  beginMedicationRequestDispatch(id: string) {
    return MedicationRequest.findOneAndUpdate(
      { _id: id, status: "pending" },
      { $set: { status: "dispatching" } },
      { new: true },
    );
  }
  findMedicationRequestById(id: string) { return MedicationRequest.findById(id); }
  findAvailableLots(medicationId: unknown) {
    return InventoryLot.find({
      medication: medicationId as Types.ObjectId,
      quantity: { $gt: 0 },
      expirationDate: { $gt: new Date() },
    }).sort({ expirationDate: 1, _id: 1 });
  }
  async decrementLot(id: unknown, quantity: number) {
    const result = await InventoryLot.updateOne(
      { _id: id, quantity: { $gte: quantity } },
      { $inc: { quantity: -quantity } },
    );
    return result.modifiedCount === 1;
  }
  async completeMedicationRequest(id: unknown, dispatchedLots: Array<{ lot: unknown; quantity: number }>) {
    const result = await MedicationRequest.findOneAndUpdate(
      { _id: id, status: "dispatching" },
      { $set: { status: "dispatched", dispatchedAt: new Date(), dispatchedLots } },
      { new: true },
    );
    if (!result) {
      throw new InventoryDomainError("No se pudo actualizar el estado de la solicitud", 409);
    }
    return result;
  }
  async resetMedicationRequest(id: unknown) {
    const result = await MedicationRequest.updateOne(
      { _id: id, status: "dispatching" },
      { $set: { status: "pending" } },
    );
    return result.modifiedCount === 1;
  }
  async restoreLot(id: unknown, quantity: number) {
    await InventoryLot.updateOne({ _id: id }, { $inc: { quantity } });
  }
  findMedicationRequestWithDetails(id: unknown) {
    return MedicationRequest.findById(id).populate("medication").populate("dispatchedLots.lot");
  }
  lotExists(id: string) { return InventoryLot.exists({ _id: id }).then(Boolean); }
  async adjustLotStock(id: string, adjustment: number) {
    const filter: Record<string, unknown> = { _id: id };
    if (adjustment < 0) filter.quantity = { $gte: Math.abs(adjustment) };
    const found = await InventoryLot.findOneAndUpdate(
      filter,
      { $inc: { quantity: adjustment } },
      { new: true, runValidators: true },
    ).populate("medication");
    return { found };
  }
  getLowStockLots() {
    return InventoryLot.find({ $expr: { $lte: ["$quantity", "$minimumStock"] } })
      .populate("medication")
      .sort({ quantity: 1, expirationDate: 1 });
  }
  getExpiringLots(now: Date, cutoff: Date) {
    return InventoryLot.find({ expirationDate: { $gte: now, $lte: cutoff } })
      .populate("medication")
      .sort({ expirationDate: 1 });
  }
}
