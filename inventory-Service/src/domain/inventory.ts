export class InventoryDomainError extends Error {
  constructor(
    message: string,
    public readonly statusCode: number,
  ) {
    super(message);
    this.name = "InventoryDomainError";
  }
}

export interface MedicationInput {
  name: string;
  activeIngredient: string;
  presentation: string;
  concentration: string;
}

export interface LotInput {
  medication: string;
  lotCode: string;
  quantity: number;
  minimumStock: number;
  expirationDate: string;
}

export interface MedicationRequestInput {
  medication: string;
  quantity: number;
  requestedBy: string;
}
