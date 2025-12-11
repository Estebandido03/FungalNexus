package org.fungalnexus;

public final class Configuracion {
    // --- Configuración General del Mapa ---
    public static final int GRID_SIZE = 30; // Tamaño en píxeles de cada celda del grid invisible
    public static final int MAPA_WIDTH = 800;
    public static final int MAPA_HEIGHT = 600;

    // --- Configuración de Nodos ---
    public static final double RADIO_NODO_ESTANDAR = 11.0;
    public static final double RADIO_NUCLEO_INICIAL = 15.0;

    // --- Configuración de Juego/Balance ---
    public static final double FACTOR_PROPAGACION_BACTERIA = 0.01; // Probabilidad de propagación por ciclo
    public static final double DANO_BACTERIA_POR_CICLO = 4.0; // Daño al nodo infectado
    public static final double COSTO_DEFENSA_POR_COMBATE = 6.0;
    public static final double CAPACIDAD_MAXIMA_DEFENSA = 100.0;
    public static final double TASA_SANACION_INFECCION = 0.02;
    public static final double PROBABILIDAD_INFECCION_EXTERNA = 0.024;
    public static final int CICLO_GRACIA_INICIAL = 60;
    public static final double NODO_RADIO = 10;

    private Configuracion() {
        // Evitar instanciación
    }
}