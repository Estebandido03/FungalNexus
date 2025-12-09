package org.fungalnexus;

public class Hifa {
    private String destinoNombre;
    private String origenNombre;
    private int capacidadMaxima;
    private int costoMantenimiento; // Gasto de nutrientes por ciclo
    private int salud; // Para futura degradación

    public Hifa(String destino, int capacidad, int costo) {
        this.destinoNombre = destino;
        this.capacidadMaxima = capacidad;
        this.costoMantenimiento = costo;
        this.salud = 100;
    }

    // Getters y Setters necesarios
    public String getDestinoNombre() {
        return destinoNombre;
    }
    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }
    public int getCostoMantenimiento() {
        return costoMantenimiento;
    }

    public void setCapacidadMaxima(int nuevaCapacidad) {
        this.capacidadMaxima = nuevaCapacidad;
    }
}