package org.fungalnexus;

public final class Configuracion {
    // --- Configuración General del Mapa ---
    public static final int GRID_SIZE = 30; // Tamaño en píxeles de cada celda del grid invisible
    public static final int MAPA_WIDTH = 800;
    public static final int MAPA_HEIGHT = 600;

    // --- Configuración de Nodos ---
    public static final double NUCLEO_RADIO = 20.0;
    public static final double NODO_RADIO = 10.0;

    // --- Configuración de Juego/Balance ---
    public static final double FACTOR_PROPAGACION_BACTERIA = 0.004; // Probabilidad de propagación por ciclo
    public static final double DANO_BACTERIA_POR_CICLO = 6.0; // Daño al nodo infectado
    public static final double COSTO_DEFENSA_POR_COMBATE = 2.0; // Cuántas defensas se consumen para curar

    private Configuracion() {
        // Evitar instanciación
    }
}