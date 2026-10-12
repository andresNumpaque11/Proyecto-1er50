import mongoose from "mongoose";

export async function connectDB(): Promise<void> {
    const mongoUrl= process.env.MONGO_URL;

    if(!mongoUrl){
        throw new Error('La variable MONGO_URL no está configurada');
    }
    try {
        await mongoose.connect(mongoUrl);
        console.log('Mongo Conectado Correctamente');

    } catch (error) {
        console.error('Error al conectar Con MongoDB:', error)
        throw error;
    }
}