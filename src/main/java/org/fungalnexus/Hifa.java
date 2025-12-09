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

    //Metodo para aumentar capacidad (Mejora con EXP)
    public void aumentarCapacidad(int incremento) {
        this.capacidadMaxima += incremento;
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
    // Añadido para deterioro
    public void setCostoMantenimiento(int costo) {
        this.costoMantenimiento = costo;
    }
    public void setCapacidadMaxima(int nuevaCapacidad) {
        this.capacidadMaxima = nuevaCapacidad;
    }
    public String getOrigenNombre(){
        return origenNombre;
    }
}