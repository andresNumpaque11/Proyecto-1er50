import { Schema, model, type InferSchemaType } from "mongoose";

const medicationRequestSchema = new Schema(
  {
    medication: {
      type: Schema.Types.ObjectId,
      ref: "Medication",
      required: true,
      index: true,
    },
    quantity: {
      type: Number,
      required: true,
      min: 1,
      validate: {
        validator: Number.isInteger,
        message: "La cantidad debe ser un número entero",
      },
    },
    requestedBy: {
      type: String,
      required: true,
      trim: true,
    },
    status: {
      type: String,
      enum: ["pending", "dispatching", "dispatched"],
      default: "pending",
      required: true,
    },
    dispatchedAt: {
      type: Date,
    },
    dispatchedLots: [
      {
        lot: {
          type: Schema.Types.ObjectId,
          ref: "InventoryLot",
          required: true,
        },
        quantity: {
          type: Number,
          required: true,
          min: 1,
        },
      },
    ],
  },
  {
    timestamps: true,
    versionKey: false,
  },
);

export type MedicationRequestDocument = InferSchemaType<
  typeof medicationRequestSchema
>;

export const MedicationRequest = model(
  "MedicationRequest",
  medicationRequestSchema,
);
