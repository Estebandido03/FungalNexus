package org.fungalnexus;

import java.util.HashMap;
import java.util.Map;

public class Centro {
    private String nombre;
    private String tipo; // "Central", "Extraccion", "Produccion", etc.
    private int salud;
    private int expAcumulada;
    private Map<String, Integer> inventario;
    private Map<String, Integer> costosOperacionales;

    private int yacimientoRestante;
    public static final int CAPACIDAD_MAXIMA_YACIMIENTO = 1208;

    public Centro(String nombre, String tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.inventario = new HashMap<>();
        this.costosOperacionales = new HashMap<>();
        // La fuente de extracción tendrá un recurso inicial para extraer
        if ("Central".equals(tipo)) {
            this.salud = 100;
            this.expAcumulada = 50;
            // Costos para el núcleo
            costosOperacionales.put("Nutrientes", 0);
        } else if ("Extraccion".equals(tipo)) {
            this.salud = 50;
            this.expAcumulada = 0;
            // Costo operacional local
            costosOperacionales.put("Nutrientes", 2);
            // Inicialización del yacimiento
            this.yacimientoRestante = CAPACIDAD_MAXIMA_YACIMIENTO;
        }
    }

    // Getters y Setters necesarios
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }

    public int getSalud() { return salud; }
    public void setSalud(int salud) { this.salud = salud; }

    public int getExpAcumulada() { return expAcumulada; }
    public void sumarExp(int cantidad) { this.expAcumulada += cantidad; }
    public void restarExp(int cantidad) { this.expAcumulada -= cantidad; }

    public Map<String, Integer> getInventario() { return inventario; }

    // Simulación del costo de operación local
    public int getCostoOperacional(String recurso) {
        if (this.tipo.equals("Extraccion") && recurso.equals("Nutrientes")) {
            return 2; // Costo fijo de 2 unidades por ciclo para operar
        }
        return 0;
    }

    public int getYacimientoRestante() {
        return yacimientoRestante;
    }

    public void restarYacimiento(int cantidad) {
        this.yacimientoRestante -= cantidad;
        if (yacimientoRestante < 0) {
            yacimientoRestante = 0;
        }
    }
}