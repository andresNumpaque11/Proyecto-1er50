export interface InventoryRepositoryPort {
  createMedication(input: unknown): Promise<unknown>;
  getMedications(): Promise<unknown>;
  findMedicationById(id: string): Promise<{ _id: unknown; active: boolean } | null>;
  findActiveMedication(id: string): Promise<{ _id: unknown } | null>;
  createLot(input: unknown): Promise<unknown>;
  getLots(): Promise<unknown>;
  createMedicationRequest(input: unknown): Promise<unknown>;
  beginMedicationRequestDispatch(id: string): Promise<unknown>;
  findMedicationRequestById(id: string): Promise<unknown>;
  findAvailableLots(medicationId: unknown): Promise<Array<unknown>>;
  decrementLot(id: unknown, quantity: number): Promise<boolean>;
  completeMedicationRequest(id: unknown, dispatchedLots: Array<{ lot: unknown; quantity: number }>): Promise<unknown>;
  resetMedicationRequest(id: unknown): Promise<boolean>;
  restoreLot(id: unknown, quantity: number): Promise<void>;
  findMedicationRequestWithDetails(id: unknown): Promise<unknown>;
  lotExists(id: string): Promise<boolean>;
  adjustLotStock(id: string, adjustment: number): Promise<{ found: unknown }>;
  getLowStockLots(): Promise<unknown>;
  getExpiringLots(now: Date, cutoff: Date): Promise<unknown>;
}
