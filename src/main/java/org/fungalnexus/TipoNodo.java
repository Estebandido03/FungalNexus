package org.fungalnexus;

public enum TipoNodo {
    NUCLEO (100.0,100.0,0.0,0.0,0.0),
    EXTRACTOR (150.0,0.0,75.0,5.0,0.0),
    ALMACENAMIENTO (75.0,50.0,100.0,0.0,0.0),
    DEFENSA (60.0,0.0,100.0,0.0,5.0);

    // Propiedades internas del Enum
    private final double saludBase;
    private final double capacidadNutriente;
    private final double costo;
    private final double tasaProduccion; // Tasa de extracción/consumo (Nutrientes)
    private final double tasaDefensa;    // Cantidad de defensa generada

    // --- Constructor ACTUALIZADO (5 argumentos) ---
    TipoNodo(double saludBase, double capacidadNutriente, double costo, double tasaProduccion, double tasaDefensa) {
        this.saludBase = saludBase;
        this.capacidadNutriente = capacidadNutriente;
        this.costo = costo;
        this.tasaProduccion = tasaProduccion;
        this.tasaDefensa = tasaDefensa;
    }

    // --- Getters (Todos a double) ---
    public double getSaludBase() {
        return saludBase;
    }

    public double getCapacidadNutriente() {
        return capacidadNutriente;
    }

    public double getCosto() {
        return costo;
    }

    public double getTasaProduccion() {
        return tasaProduccion;
    }

    public double getTasaDefensa() {
        return tasaDefensa;
    }
}