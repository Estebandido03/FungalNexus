package org.fungalnexus;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;

public class ParticulaRecurso extends Circle {

    private final double velocidad = 3.0; // Velocidad en píxeles por ciclo de actualización
    private double destinoX;
    private double destinoY;
    private final TipoRecurso tipo; // Para diferenciar Nutrientes de Defensas
    private final List<Nodo> ruta; // La lista de nodos a seguir (Ej: [Extractor, Almacen, Nucleo])
    private int indiceRuta; // El índice del nodo de destino actual en la lista

    public ParticulaRecurso(double startX, double startY, List<Nodo> ruta, TipoRecurso tipo) {
        super(startX, startY, 3); // Radio pequeño de 3px
        this.ruta = ruta;
        this.indiceRuta = 1;
        this.tipo = tipo;
        // Asignar destinoX y destinoY al segundo nodo de la ruta
        if (ruta != null && ruta.size() > 1) {
            Nodo siguiente = ruta.get(indiceRuta);
            this.destinoX = siguiente.getX();
            this.destinoY = siguiente.getY();
        } else {
            // Manejo de error o ruta de 1 nodo (si el extractor es el núcleo)
            this.destinoX = startX;
            this.destinoY = startY;
        }

        // Asignar color según el tipo de recurso
        switch (tipo) {
            case NUTRIENTE:
                setFill(Color.YELLOW);
                break;
            case DEFENSA:
                setFill(Color.CYAN); // O un color que te guste para defensas
                break;
        }
    }

    // --- Metodo de Actualización ---
    public boolean mover() {
        double dx = destinoX - getCenterX();
        double dy = destinoY - getCenterY();
        double distancia = Math.sqrt(dx * dx + dy * dy);

        if (distancia < velocidad) {
            // La partícula llegó a un nodo intermedio (o al núcleo)

            // 1. Mover al siguiente nodo en la ruta
            indiceRuta++;

            if (indiceRuta < ruta.size()) {
                // Hay más nodos en la ruta, actualizar el destino
                Nodo siguiente = ruta.get(indiceRuta);
                this.destinoX = siguiente.getX();
                this.destinoY = siguiente.getY();
                return false; // Aún en movimiento
            } else {
                // Llegó al final de la ruta (el Núcleo)
                return true; // Ha llegado y debe ser eliminada
            }
        } else {
            // Moverse hacia el destino actual
            setCenterX(getCenterX() + dx / distancia * velocidad);
            setCenterY(getCenterY() + dy / distancia * velocidad);
            return false;
        }
    }
}