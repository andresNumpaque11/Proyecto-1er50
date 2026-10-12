import { Schema, model, Types, InferSchemaType } from "mongoose";
import { Medication } from "./Medication";

const inventoryLotSchema = new Schema(
    {
        medication: {
            type: Schema.Types.ObjectId,
            ref: 'Medication',
            required: true,
            index: true,
        },
        lotCode:{
            type: String,
            required: true,
            trim: true,

        },
        quantity:{
            type:Number,
            required: true,
            min: 0,
            validate: {
                validator: Number.isInteger,
                message: 'La cantidad debe ser un numero entero',
            },
        },
        minimumStock: {
            type: Number,
            required: true,
            default:10,
            min: 10,
            validate: {
                validator: Number.isInteger,
                message: 'El stock minimo debe ser un numero entero',
            },
        },
        expirationDate:{
            type: Date,
            required: true,
        },
    },
    {
        timestamps: true,
        versionKey: false,
    },
);

inventoryLotSchema.index(
    {medication: 1, lotCode: 1},
    {unique: true},
);

export type InventoryLotDocument = InferSchemaType<typeof inventoryLotSchema>;

export const InventoryLot = model(
    'InventoryLot', inventoryLotSchema,
);