import { Request, Response } from "express";
import {
  createMedication,
  createLot,
  getLots,
  getMedications,
  adjustLotStock,
  getExpiringLots,
  getLowStockLots,
  InventoryServiceError,
  createMedicationRequest,
  dispatchMedicationRequest,
} from "../service/inventory.service";

interface MedicationBody {
  name: string;
  activeIngredient: string;
  presentation: string;
  concentration: string;
}

interface LotBody {
  medication: string;
  lotCode: string;
  quantity: number;
  minimumStock?: number;
  expirationDate: string;
}

interface MedicationRequestBody {
  medication: string;
  quantity: number;
  requestedBy: string;
}

function handleError(res: Response, error: unknown, defaultMessage: string) {
  const message = error instanceof Error ? error.message : defaultMessage;
  const statusCode =
    error instanceof InventoryServiceError ? error.statusCode : 400;
  res.status(statusCode).json({ message });
}

export async function createMedicationController(
  req: Request,
  res: Response,
): Promise<void> {
  try {
    const medicationData = req.body as MedicationBody;
    const medication = await createMedication(medicationData);
    res.status(201).json(medication);
  } catch (error) {
    handleError(res, error, "No se pudo crear el medicamento");
  }
}

export async function getMedicationsController(
  _req: Request,
  res: Response,
): Promise<void> {
  try {
    const medications = await getMedications();
    res.status(200).json(medications);
  } catch (error) {
    handleError(res, error, "No se pudieron obtener los medicamentos");
  }
}

export async function createLotController(
  req: Request,
  res: Response,
): Promise<void> {
  try {
    const lotData = req.body as LotBody;
    const lot = await createLot({
      ...lotData,
      minimumStock: lotData.minimumStock ?? 10,
    });
    res.status(201).json(lot);
  } catch (error) {
    handleError(res, error, "No se pudo crear el lote");
  }
}

export async function getLotsController(
  _req: Request,
  res: Response,
): Promise<void> {
  try {
    const lots = await getLots();
    res.status(200).json(lots);
  } catch (error) {
    handleError(res, error, "No se pudieron obtener los lotes");
  }
}

export async function createMedicationRequestController(
  req: Request,
  res: Response,
): Promise<void> {
  try {
    const requestData = req.body as MedicationRequestBody;
    const medicationRequest = await createMedicationRequest(requestData);
    res.status(201).json(medicationRequest);
  } catch (error) {
    handleError(res, error, "No se pudo crear la solicitud de medicamentos");
  }
}

export async function dispatchMedicationRequestController(
  req: Request,
  res: Response,
): Promise<void> {
  try {
    const id = req.params.id;
    if (typeof id !== "string") {
      throw new InventoryServiceError("El id de la solicitud no es válido", 400);
    }
    const medicationRequest = await dispatchMedicationRequest(id);
    res.status(200).json(medicationRequest);
  } catch (error) {
    handleError(res, error, "No se pudo despachar la solicitud");
  }
}

export async function adjustLotStockController(
  req: Request,
  res: Response,
): Promise<void> {
  try {
    const { adjustment } = req.body as { adjustment?: unknown };
    const id = req.params.id;
    if (typeof id !== "string") {
      throw new InventoryServiceError("El id del lote no es válido", 400);
    }
    const lot = await adjustLotStock(id, adjustment);
    res.status(200).json(lot);
  } catch (error) {
    handleError(res, error, "No se pudo actualizar el stock del lote");
  }
}

export async function getLowStockLotsController(
  _req: Request,
  res: Response,
): Promise<void> {
  try {
    const lots = await getLowStockLots();
    res.status(200).json(lots);
  } catch (error) {
    handleError(res, error, "No se pudieron obtener las alertas de bajo stock");
  }
}

export async function getExpiringLotsController(
  req: Request,
  res: Response,
): Promise<void> {
  try {
    const days =
      req.query.days === undefined
        ? 30
        : typeof req.query.days === "string"
          ? Number(req.query.days)
          : Number.NaN;
    const lots = await getExpiringLots(days);
    res.status(200).json(lots);
  } catch (error) {
    handleError(res, error, "No se pudieron obtener los lotes próximos a vencer");
  }
}
