import { Schema, model, type InferSchemaType } from "mongoose";

const medicationSchema = new Schema(
  {
    name: {
      type: String,
      required: true,
      trim: true,
    },
    activeIngredient: {
      type: String,
      required: true,
      trim: true,
    },
    presentation: {
      type: String,
      required: true,
      trim: true,
    },
    concentration: {
      type: String,
      required: true,
      trim: true,
    },
    active: {
      type: Boolean,
      default: true,
    },
  },
  {
    timestamps: true,
    versionKey: false,
  },
);

export type MedicationDocument = InferSchemaType<typeof medicationSchema>;

export const Medication = model('Medication', medicationSchema);