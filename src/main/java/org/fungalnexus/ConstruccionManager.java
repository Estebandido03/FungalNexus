package org.fungalnexus;

public class ConstruccionManager {

    private TipoNodo nodoSeleccionado = null;
    private double costoSeleccionado = 0.0;
    private double tasaProduccion = 0.0;

    /**
     * Define el modo de construcción actual.
     */
    public void seleccionarConstruccion(TipoNodo tipo) {
        this.nodoSeleccionado = tipo;

        this.costoSeleccionado = tipo.getCosto();
        // Determinar la tasa según el tipo
        if (tipo == TipoNodo.EXTRACTOR) {
            this.tasaProduccion = tipo.getTasaProduccion(); // (Tasa Nutrientes)
        } else if (tipo == TipoNodo.DEFENSA) {
            this.tasaProduccion = tipo.getTasaDefensa();   // (Tasa Defensa)
        } else {
            this.tasaProduccion = 0.0;
        }

        System.out.println("Modo Construcción Activado: " + tipo + ". Costo: " + costoSeleccionado);
    }

    public void cancelarConstruccion() {
        this.nodoSeleccionado = null;
        this.costoSeleccionado = 0.0;
        this.tasaProduccion = 0.0;
        System.out.println("Modo Construcción Desactivado.");
    }

    public boolean estaEnModoConstruccion() {
        return nodoSeleccionado != null;
    }

    // --- Getters ---
    public TipoNodo getNodoSeleccionado() {
        return nodoSeleccionado;
    }

    public double getCostoSeleccionado() {
        return costoSeleccionado;
    }

    public double getTasaProduccion() {
        return tasaProduccion;
    }
}
