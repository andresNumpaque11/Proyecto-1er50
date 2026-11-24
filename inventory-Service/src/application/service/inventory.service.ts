import { InventoryUseCases } from "../../domain/ports/inventory.use-cases";
import { InventoryDomainError, type LotInput, type MedicationInput, type MedicationRequestInput } from "../../domain/inventory";
import { MongooseInventoryRepository } from "../../infrastructure/persistence/MongooseInventoryRepository";

export { InventoryDomainError as InventoryServiceError };

const useCases = new InventoryUseCases(new MongooseInventoryRepository());

export const createMedication = (input: MedicationInput) => useCases.createMedication(input);
export const getMedications = () => useCases.getMedications();
export const createLot = (input: LotInput) => useCases.createLot(input);
export const getLots = () => useCases.getLots();
export const createMedicationRequest = (input: MedicationRequestInput) => useCases.createMedicationRequest(input);
export const dispatchMedicationRequest = (id: string) => useCases.dispatchMedicationRequest(id);
export const adjustLotStock = (id: string, adjustment: unknown) => useCases.adjustLotStock(id, adjustment);
export const getLowStockLots = () => useCases.getLowStockLots();
export const getExpiringLots = (days: number) => useCases.getExpiringLots(days);
