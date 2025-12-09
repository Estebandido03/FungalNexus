package org.fungalnexus;

import javafx.animation.AnimationTimer;
import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class CentroView extends Circle {
    private Centro modelo;

    public CentroView(Centro centro, double x, double y, double radio) {
        super(x, y, radio);
        this.modelo = centro;
        this.setStroke(Color.BLACK);
        actualizarVisualizacion();
    }

    public void actualizarVisualizacion() {
        if (modelo.getSalud() <= 20) {
            this.setFill(Color.DARKRED);
        } else if (modelo.getTipo().equals("Central")) {
            this.setFill(Color.PURPLE);
        } else if (modelo.getTipo().equals("Extraccion")) {
            this.setFill(Color.GREEN); // Extracción de Nutrientes = Verde
        } else {
            this.setFill(Color.BROWN);
        }
    }
}

class FlujoParticle extends Circle {
    public FlujoParticle(Color color) {
        super(3);
        this.setFill(color);
    }
}

// Motor de juego y renderizado
public class MainApp extends Application {

    // Variables de Estado del juego
    private ColoniaGrafo colonia;
    private Pane root;
    private Map<String, CentroView> vistasDeCentros = new HashMap<>();
    private Map<String, Point2D> coordenadasDeCentros = new HashMap<>();
    private Text hudText; // Para mostrar EXP y Salud
    private double TILE_SIZE = 50.0;
    private String gameState = "WAITING_FOR_SETUP";
    private long startTime = 0;
    private boolean isGameOver = false;
    private final long HEALTH_DECAY_START_TIME = 20_000_000_000L;
    private boolean modoConstruccion = false;
    private final double TOLERANCIA_CLIC = 15.0;
    private final int COSTO_EXPANSION_EXTRACCION_NUEVO = 40;

    // Costos y parametros del juego
    private final int COSTO_EXPANSION_EXTRACCION = 40;
    private final int COSTO_MANTENIMIENTO_BASE_NUCLEO = 8;
    private final int COSTO_MANTENIMIENTO_HIFA_BASE = 2;
    private final double PIXELES_POR_UNIDAD_DE_COSTO = 100.0;
    private final int COSTO_MEJORA_HIFA = 10;
    private final int MAX_EXP_CAPACITY = 500;

    private final double LIMITE_CONSTRUCCION_DISTANCIA = 3.0 * TILE_SIZE;

    @Override
    public void start(Stage primaryStage) {
        colonia = new ColoniaGrafo();
        root = new Pane();
        root.setPrefSize(1000, 700);
        root.setStyle("-fx-background-color: #2b1c09");

        setupGrafoInicial();
        iniciarHUD();
        iniciarCicloSimulacion();

        Scene mainScene = new Scene(root, 1000, 700);

        primaryStage.setTitle("Fungal Nexus - Prototipo Interactivo");
        primaryStage.setScene(mainScene);

        iniciarInteraccion(mainScene);

        primaryStage.show();
        startTime = System.nanoTime(); // Iniciar el temporizador
    }

    // Setup inicial del Grafo logico
    private void setupGrafoInicial() {
        // Único nodo inicial con EXP de arranque (50 EXP)
        Centro nucleo = new Centro("NucleoCentral", "Central");

        colonia.agregarCentro(nucleo);

        // Posicionamiento en el centro del mapa
        coordenadasDeCentros.put("NucleoCentral", new Point2D(root.getPrefWidth() / 2, root.getPrefHeight() / 2));

        dibujarGrafo(); // Dibuja solo el núcleo inicialmente
        gameState = "WAITING_FOR_EXTRACTION_PLACEMENT";

        System.out.println("EXP Incial: " + nucleo.getExpAcumulada());
    }

    // Dibujo y visualizacion
    private void dibujarGrafo() {
        // 1. Guardar el objeto Text del HUD
        Text tempHudText = this.hudText;

        root.getChildren().clear(); // Limpiar el panel

        // Dibujar líneas de grid para referencia
        dibujarGrid();

        // Dibujar Nodos (Centros)
        for (Centro centro : colonia.obtenerTodosLosCentros()) {
            Point2D coords = coordenadasDeCentros.get(centro.getNombre());
            double radio = centro.getTipo().equals("Central") ? 30 : 20;
            CentroView view = new CentroView(centro, coords.getX(), coords.getY(), radio);
            vistasDeCentros.put(centro.getNombre(), view);
            root.getChildren().add(view);
        }

        // Dibujar Hifas
        for (String origen : colonia.obtenerNombresDeCentros()) {
            for (Hifa hifa : colonia.obtenerHifas(origen)) {
                Point2D coordsOrigen = coordenadasDeCentros.get(origen);
                Point2D coordsDestino = coordenadasDeCentros.get(hifa.getDestinoNombre());

                if (coordsOrigen != null && coordsDestino != null) {
                    Line hifaLine = new Line(coordsOrigen.getX(), coordsOrigen.getY(), coordsDestino.getX(), coordsDestino.getY());
                    hifaLine.setStrokeWidth(hifa.getCapacidadMaxima() / 5.0);
                    hifaLine.setStroke(Color.SADDLEBROWN);
                    root.getChildren().add(0, hifaLine);
                }
            }
        }

        // Restaurar el HUD al final para que esté siempre encima
        if (tempHudText != null) {
            root.getChildren().add(tempHudText);
        }
    }

    private void dibujarGrid() {
        for (double i = 0; i < root.getPrefWidth(); i += TILE_SIZE) {
            Line line = new Line(i, 0, i, root.getPrefHeight());
            line.setStroke(Color.LIGHTGRAY);
            root.getChildren().add(line);
        }
        for (double i = 0; i < root.getPrefHeight(); i += TILE_SIZE) {
            Line line = new Line(0, i, root.getPrefWidth(), i);
            line.setStroke(Color.LIGHTGRAY);
            root.getChildren().add(line);
        }
    }

    private void iniciarHUD() {
        hudText = new Text(10, 20, "Inicia la partida...");
        hudText.setFill(Color.WHITE);
        root.getChildren().add(hudText);
    }

    private void actualizarHUD(long elapsedTime) {
        Centro extractorPrincipal = colonia.obtenerTodosLosCentros().stream()
                .filter(c -> c.getTipo().equals("Extraccion"))
                .findFirst()
                .orElse(null);

        String infoYacimiento = "";
        if (extractorPrincipal != null) {
            infoYacimiento = String.format(" | Yacimiento: %d/%d", extractorPrincipal.getYacimientoRestante(), extractorPrincipal.CAPACIDAD_MAXIMA_YACIMIENTO);
        }

        Centro nucleo = colonia.obtenerCentro("NucleoCentral");
        long seconds = elapsedTime / 1_000_000_000;

        // --- 1. Definición de Estados del Sistema ---
        boolean sistemaActivo = colonia.obtenerTodosLosCentros().size() > 1;

        // Simplificado: ¿Existe alguna Hifa que aún tenga la capacidad base de 10?
        boolean hifaEnNivelBase = colonia.obtenerTodasLasHifas().stream()
                .flatMap(List::stream)
                .anyMatch(h -> h.getCapacidadMaxima() == 10);

        // Simplificado: ¿Existe alguna Hifa que ya esté mejorada (> 10)?
        boolean hifaMejorada = colonia.obtenerTodasLasHifas().stream()
                .flatMap(List::stream)
                .anyMatch(h -> h.getCapacidadMaxima() > 10);


        // --- 2. Lógica para el Mensaje de Acción ---
        String estadoAccion;

        if (!sistemaActivo) {
            // A. Estado Inicial: Solo Núcleo
            estadoAccion = "Costo Extracción: " + COSTO_EXPANSION_EXTRACCION + " EXP. ¡CLICK para construir!";

        } else if (hifaEnNivelBase && nucleo.getExpAcumulada() >= 10) {
            // B. Estado de Equilibrio: Puede pagar la mejora
            estadoAccion = "¡EXP suficiente! Haz CLIC en la línea Hifa para MEJORAR (Costo: 10 EXP).";

        } else if (hifaEnNivelBase && nucleo.getExpAcumulada() < 10) {
            // C. Estado de Equilibrio: Aún no puede pagar la mejora
            estadoAccion = "Sistema en Equilibrio (0 EXP/s). ¡Necesitas 10 EXP para la Mejora!";

        } else if (hifaMejorada) {
            // D. Estado de Crecimiento: Mejora aplicada
            estadoAccion = "Sistema en CRECIMIENTO (+8 EXP/s). ¡Ahorrando EXP para tu próxima expansión!";

        } else {
            // E. Fallback (Si se pierden los casos anteriores, al menos decimos algo)
            estadoAccion = "Sistema en funcionamiento. No hay acciones urgentes.";
        }


        // --- 3. Actualización final del HUD ---
        hudText.setText(String.format(
                "Tiempo: %d s | Salud Núcleo: %d%s\n" + // <-- Usar %s para infoYacimiento
                        "EXP: %d | Nutrientes: %d\n" +
                        "Gasto Mantenimiento: %d / ciclo\n" +
                        "Estado: %s",
                seconds,
                nucleo.getSalud(),
                infoYacimiento,
                nucleo.getExpAcumulada(),
                nucleo.getInventario().getOrDefault("Nutrientes", 0),
                costoTotalMantenimiento(),
                estadoAccion
        ));
    }

    private int costoTotalMantenimiento() {
        return colonia.obtenerTodasLasHifas().stream()
                .flatMap(List::stream)
                .mapToInt(Hifa::getCostoMantenimiento)
                .sum() + COSTO_MANTENIMIENTO_BASE_NUCLEO;
    }

    // Interaccion (click para continuar)
    private void iniciarInteraccion(Scene scene) {

        // --- 1. Manejador de Teclado (Requiere la Scene) ---
        scene.setOnKeyPressed(event -> { // <-- Se usa la 'scene' recibida
            if (event.getCode() == KeyCode.E) {
                modoConstruccion = !modoConstruccion;

                String mensaje = modoConstruccion
                        ? "Modo Construcción ACTIVO. ¡Haz clic en el mapa para colocar el Extractor!"
                        : "Modo Construcción DESACTIVADO.";

                System.out.println(mensaje);
            }
        });

        // --- 2. Manejador de Clic (Existente) ---
        root.setOnMouseClicked(e -> {
            Point2D clickPoint = new Point2D(e.getX(), e.getY());
            Centro nucleo = colonia.obtenerCentro("NucleoCentral"); // Obtenemos el núcleo aquí

            // Lógica de Colocación de un NUEVO Extractor
            if (modoConstruccion) {
                modoConstruccion = false;
                construirCentroExtractor(e.getX(), e.getY());
                return;
            }

            // Lógica de Mejora de Hifa
            Hifa hifaClickeada = buscarHifaEnCoordenadas(clickPoint);

            if (hifaClickeada != null) {
                // Verificar si el costo es válido y la Hifa no ha sido mejorada.
                if (nucleo.getExpAcumulada() >= COSTO_MEJORA_HIFA && hifaClickeada.getCapacidadMaxima() == 10) {

                    // Aplicar Mejora
                    nucleo.restarExp(COSTO_MEJORA_HIFA);
                    hifaClickeada.setCapacidadMaxima(14);

                    System.out.println(" Hifa mejorada (" + hifaClickeada.getOrigenNombre() + " -> " + hifaClickeada.getDestinoNombre() + "): Capacidad 14 u/s.");
                    dibujarGrafo(); // Redibujar para mostrar si hay cambio visual en la Hifa
                    return;

                } else if (hifaClickeada.getCapacidadMaxima() > 10) {
                    System.out.println(" Esta Hifa ya está mejorada.");
                } else {
                    System.err.println(" EXP insuficiente para la mejora (Necesitas " + COSTO_MEJORA_HIFA + " EXP).");
                }
                return; // Bloquea otros clics si se manejó la Hifa
            }

            // Lógica para la CONSTRUCCIÓN INICIAL (Si aún la usas)
            if (gameState != null && gameState.equals("WAITING_FOR_EXTRACTION_PLACEMENT")) {
                construirCentroExtractor(e.getX(), e.getY());
            }
        });
    }

    // Busca el centro existente más cercano a las coordenadas de construcción
    private String buscarCentroMasCercano(double x, double y) {
        Point2D nuevoPunto = new Point2D(x, y);
        double distanciaMinima = Double.MAX_VALUE;
        String centroCercano = null;

        for (Map.Entry<String, Point2D> entry : coordenadasDeCentros.entrySet()) {
            String nombreCentro = entry.getKey();
            Point2D coordExistente = entry.getValue();

            double distancia = nuevoPunto.distance(coordExistente);
            // Se usa < para asegurarse de que el nuevo nodo no se conecte consigo mismo
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                centroCercano = nombreCentro;
            }
        }
        return centroCercano;
    }

    private void construirCentroExtractor(double x, double y) {
        Centro nucleo = colonia.obtenerCentro("NucleoCentral");

        // 1. Verificación de Costo de EXP
        if (nucleo.getExpAcumulada() < COSTO_EXPANSION_EXTRACCION) {
            System.err.println("EXP insuficiente. Necesitas " + COSTO_EXPANSION_EXTRACCION + " EXP.");
            return;
        }

        // 2. Cálculo de Colocación en el Grid
        double gridX = Math.round(x / TILE_SIZE) * TILE_SIZE;
        double gridY = Math.round(y / TILE_SIZE) * TILE_SIZE;

        // 3. Verificación de Colisión y Proximidad
        if (coordenadasDeCentros.containsValue(new Point2D(gridX, gridY))) {
            System.err.println("Fallo de Construcción: Esta casilla ya está ocupada.");
            return;
        }

        if (!estaCercaDeCentroExistente(gridX, gridY)) {
            System.err.println("Fallo de Construcción: Debe construir cerca de un Centro existente (máx. 3 tiles).");
            return;
        }

        // 4. Determinar el Centro de Conexión y Deducción de EXP
        String centroOrigen = "Extraccion-" + colonia.obtenerTodosLosCentros().size();
        String centroDestino = buscarCentroMasCercano(gridX, gridY);

        if (centroDestino == null) {
            System.err.println("Error: No se encontró un centro al cual conectarse.");
            return;
        }

        nucleo.restarExp(COSTO_EXPANSION_EXTRACCION);

        // 5. Creación del Nuevo Centro
        Centro nuevoCentro = new Centro(centroOrigen, "Extraccion");
        colonia.agregarCentro(nuevoCentro);
        coordenadasDeCentros.put(centroOrigen, new Point2D(gridX, gridY));

        // 6. CÁLCULO DEL COSTO BASADO EN DISTANCIA
        Point2D coordDestino = coordenadasDeCentros.get(centroDestino);
        // Calcular la distancia (magnitud) entre el Núcleo y la nueva posición
        double distancia = coordDestino.distance(gridX, gridY);

        // El costo de la Hifa aumenta 1 unidad por cada 100 píxeles
        int costoDistancia = (int) Math.ceil(distancia / PIXELES_POR_UNIDAD_DE_COSTO);
        int costoTotalHifa = COSTO_MANTENIMIENTO_HIFA_BASE + costoDistancia;
        costoTotalHifa = Math.max(COSTO_MANTENIMIENTO_HIFA_BASE, costoTotalHifa); // Asegura el mínimo (ej. 2)

        // 7. Conexión de la Hifa
        colonia.conectarCentros(centroOrigen, centroDestino, 10, costoTotalHifa, 100);

        // 8. Actualización
        dibujarGrafo();

        System.out.println(String.format("Extractor %s construido. Conectado a %s. Costo Hifa: %d u/s.",
                centroOrigen, centroDestino, costoTotalHifa));
    }

    private boolean estaCercaDeCentroExistente(double x, double y) {
        Point2D nuevoPunto = new Point2D(x, y);

        for (Point2D coordExistente : coordenadasDeCentros.values()) {
            if (nuevoPunto.distance(coordExistente) <= LIMITE_CONSTRUCCION_DISTANCIA) {
                return true;
            }
        }
        return false;
    }

    private boolean esClicValidoEnHifa(Point2D P, Point2D A, Point2D B) {
        double x = P.getX();
        double y = P.getY();
        double x1 = A.getX();
        double y1 = A.getY();
        double x2 = B.getX();
        double y2 = B.getY();

        // 1. Calcular la longitud al cuadrado del segmento AB
        double lengthSq = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);

        // Si la longitud es 0, no es un segmento.
        if (lengthSq == 0) return false;

        // 2. Calcular 't': Proyección del punto P sobre la línea AB (proporción a lo largo del segmento)
        double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / lengthSq;

        // 3. Clamping: Asegurar que la proyección C cae DENTRO del segmento [0, 1]
        // Si t < 0, el punto C más cercano es A. Si t > 1, el punto C más cercano es B.
        // Si 0 < t < 1, el punto C está en algún lugar del medio.
        t = Math.max(0, Math.min(1, t));

        // 4. Calcular el punto C (el punto más cercano en el segmento AB)
        double closestX = x1 + t * (x2 - x1);
        double closestY = y1 + t * (y2 - y1);

        // 5. Calcular la distancia real entre P y C
        double distSq = (x - closestX) * (x - closestX) + (y - closestY) * (y - closestY);

        // 6. Verificar si la distancia al cuadrado está dentro de la tolerancia al cuadrado
        return distSq < TOLERANCIA_CLIC * TOLERANCIA_CLIC;
    }

    private Hifa buscarHifaEnCoordenadas(Point2D clickPoint) {
        // 1. Iterar sobre TODOS los centros como posibles orígenes de una Hifa
        for (String origen : colonia.obtenerNombresDeCentros()) {

            // El Núcleo Central no tiene hifas salientes que se puedan mejorar
            if (origen.equals("NucleoCentral")) continue;

            // 2. Iterar sobre TODAS las hifas salientes de ese centro
            for (Hifa hifa : colonia.obtenerHifas(origen)) {

                // Obtenemos las coordenadas del origen y del destino (que ya no es siempre el Núcleo)
                Point2D coordOrigen = coordenadasDeCentros.get(origen);
                Point2D coordDestino = coordenadasDeCentros.get(hifa.getDestinoNombre());

                if (coordOrigen == null || coordDestino == null) continue;

                // 3. Reutilizamos el metodo de geometria para verificar la proximidad del click
                // Ahora funciona para cualquier par de coordenadas (A, B)
                if (esClicValidoEnHifa(clickPoint, coordOrigen, coordDestino)) {
                    return hifa; // ¡Encontrada la Hifa clickeada!
                }
            }
        }
        return null; // No se encontró ninguna hifa cerca del clic
    }

    // Ciclo de simulacion y logica de flujo
    private void iniciarCicloSimulacion() {
        new AnimationTimer() {
            private long lastUpdate = 0;
            private final long UPDATE_INTERVAL = 1_000_000_000L; // 1 segundo = 1 ciclo de juego

            @Override
            public void handle(long now) {
                if (isGameOver) {
                    this.stop(); // Detiene el temporizador de animación
                    return;
                }

                long elapsedTime = now - startTime;
                if (now - lastUpdate >= UPDATE_INTERVAL) {
                    simularCiclo(elapsedTime);
                    lastUpdate = now;
                }
                actualizarHUD(elapsedTime);
            }
        }.start();
    }

    private int calcularBalanceEconomicoNeto() {
        int ingresoBrutoProyectado = 0;
        int costoTotalFijoYVariable = 0;
        String recurso = "Nutrientes";

        // --- 1. Calcular Ingreso Bruto Proyectado y Costos Fijos (Núcleo y Extractores) ---
        for (Centro centro : colonia.obtenerTodosLosCentros()) {

            // A. Costo Operacional Fijo (Núcleo: 8, Extractores: 2)
            costoTotalFijoYVariable += centro.getCostoOperacional(recurso);

            // B. Ingreso Bruto Proyectado (Solo Extractores)
            if (centro.getTipo().equals("Extraccion")) {

                // Revisa si el extractor tiene alguna Hifa saliente mejorada (capacidad > 10)
                boolean isImproved = colonia.obtenerHifas(centro.getNombre()).stream()
                        .anyMatch(h -> h.getCapacidadMaxima() > 10);

                // Suma la producción proyectada (14 si mejorado, 10 si base)
                ingresoBrutoProyectado += isImproved ? 14 : 10;
            }
        }

        // --- 2. Sumar Costos Variables de Mantenimiento de Hifas ---
        for (String origen : colonia.obtenerNombresDeCentros()) {
            for (Hifa hifa : colonia.obtenerHifas(origen)) {
                // Suma el costo variable (ej. 2 + costo por distancia)
                costoTotalFijoYVariable += hifa.getCostoMantenimiento();
            }
        }

        // 3. Balance Neto
        int balanceNeto = ingresoBrutoProyectado - costoTotalFijoYVariable;

        return balanceNeto;
    }

    private void simularCiclo(long elapsedTime) {
        Centro nucleo = colonia.obtenerCentro("NucleoCentral");
        String recurso = "Nutrientes";

        if (isGameOver) {
            return;
        }

        // A. Fase de Verificación y Penalización Pre-Producción
        manejarPenalizacionPorDeficitInicial(nucleo, elapsedTime);

        // B. Fase 1: Producción Local y Depósito en Inventarios
        manejarProduccionLocal(recurso);

        // C. Fase 2: Transferencia de Recursos a Través de la Red (Ruteo)
        manejarTransferenciaDeRed(recurso);

        // D. Fase 3: Consumo y Conversión de EXP en el Núcleo
        manejarConsumoYSuperavit(nucleo, recurso);

        // E. Fase Final y Actualización
        manejarFinDeJuego(nucleo);
        vistasDeCentros.values().forEach(CentroView::actualizarVisualizacion);
    }

    private void manejarPenalizacionPorDeficitInicial(Centro nucleo, long elapsedTime) {
        boolean tieneExtractores = colonia.obtenerTodosLosCentros().stream().anyMatch(c -> c.getTipo().equals("Extraccion"));

        if (!tieneExtractores && elapsedTime > HEALTH_DECAY_START_TIME) {
            nucleo.setSalud(nucleo.getSalud() - 5);
            System.err.println("¡CRÍTICO! Falta de Nutrientes. Salud: " + nucleo.getSalud());
        }
    }

    private void manejarProduccionLocal(String recurso) {
        boolean tieneExtractores = colonia.obtenerTodosLosCentros().stream().anyMatch(c -> c.getTipo().equals("Extraccion"));

        if (!tieneExtractores) return;

        for (Centro extractor : colonia.obtenerTodosLosCentros().stream().filter(c -> c.getTipo().equals("Extraccion")).toList()) {

            // La lógica para determinar si está mejorado debe ser movida aquí.
            boolean isImproved = colonia.obtenerHifas(extractor.getNombre()).stream()
                    .anyMatch(h -> h.getCapacidadMaxima() > 10);
            int potencialBruto = isImproved ? 14 : 10;

            // Tasa de Extracción y Agotamiento del Yacimiento
            int extraidoReal = Math.min(potencialBruto, extractor.getYacimientoRestante());
            if (extraidoReal > 0) {
                extractor.restarYacimiento(extraidoReal);
            }

            // Costo Operacional Local
            int costoOperacionalLocal = extractor.getCostoOperacional(recurso);
            int recursosDisponibles = Math.max(0, extraidoReal - costoOperacionalLocal);

            // Almacenar el resultado NETO en el INVENTARIO DEL EXTRACTOR
            extractor.getInventario().put(recurso,
                    extractor.getInventario().getOrDefault(recurso, 0) + recursosDisponibles);
        }
    }

    private void manejarTransferenciaDeRed(String recurso) {
        // Itera sobre todos los centros que pueden ser el origen de un flujo
        for (String origenNombre : colonia.obtenerNombresDeCentros()) {
            Centro origen = colonia.obtenerCentro(origenNombre);

            if (origenNombre.equals("NucleoCentral")) continue;

            for (Hifa hifa : colonia.obtenerHifas(origenNombre)) {
                Centro destino = colonia.obtenerCentro(hifa.getDestinoNombre());

                int flujoMaximoHifa = hifa.getCapacidadMaxima();
                int costoMantenimientoHifa = hifa.getCostoMantenimiento();
                int disponibleEnOrigen = origen.getInventario().getOrDefault(recurso, 0);

                int flujoReal = Math.min(disponibleEnOrigen, flujoMaximoHifa);
                int ingresoNetoADestino = flujoReal - costoMantenimientoHifa;

                if (ingresoNetoADestino > 0) {
                    // Deducción y Depósito
                    origen.getInventario().put(recurso, disponibleEnOrigen - flujoReal);
                    destino.getInventario().put(recurso,
                            destino.getInventario().getOrDefault(recurso, 0) + ingresoNetoADestino);

                    // Animación Visual
                    actualizarFlujoVisual(origenNombre, hifa.getDestinoNombre(), flujoReal, recurso);
                }
            }
        }
    }

    private void manejarConsumoYSuperavit(Centro nucleo, String recurso) {
        if (colonia.obtenerTodosLosCentros().size() <= 1) return;

        final int costoOperacionalNucleo = COSTO_MANTENIMIENTO_BASE_NUCLEO;
        int nutrientesDisponibles = nucleo.getInventario().getOrDefault(recurso, 0);

        // 1. CÁLCULO DEL SUPERÁVIT POTENCIAL
        int superavitPotencial = nutrientesDisponibles - costoOperacionalNucleo;

        // C. Lógica de Pago y Falla
        if (nutrientesDisponibles >= costoOperacionalNucleo) {

            // PAGO EXITOSO: Forzamos el inventario a cero (toda la capacidad de pago/superávit se "consume")
            nucleo.getInventario().put(recurso, 0);

            // 2. PRIORIDAD: SANACIÓN Y CONVERSIÓN A EXP
            if (superavitPotencial > 0) {
                int superavitParaGastar = superavitPotencial;
                int saludActual = nucleo.getSalud();
                int puntosDeVidaFaltantes = 100 - saludActual;

                // 2a. PRIORIDAD 1: SANACIÓN
                if (puntosDeVidaFaltantes > 0) {
                    int curacionMaxima = Math.min(superavitParaGastar, puntosDeVidaFaltantes);
                    nucleo.setSalud(saludActual + curacionMaxima);
                    superavitParaGastar -= curacionMaxima;
                }

                // 2b. PRIORIDAD 2: CONVERSIÓN A EXP
                if (superavitParaGastar > 0) {
                    int expGenerada = superavitParaGastar * 1;
                    nucleo.sumarExp(expGenerada);
                }
            }
        } else {
            // FALLA DE MANTENIMIENTO CRÍTICA
            nucleo.setSalud(nucleo.getSalud() - 2);
            System.err.println("¡Alerta! Falla de mantenimiento crítico (Núcleo). Salud: " + nucleo.getSalud());
        }
    }

    private void manejarFinDeJuego(Centro nucleo) {
        if (nucleo.getSalud() <= 0) {
            nucleo.setSalud(0);
            isGameOver = true;
            System.err.println("!!! GAME OVER: El Núcleo Central ha muerto. !!!");
        }
    }

    // --- 5. ANIMACIÓN DE FLUJO ---
    private void actualizarFlujoVisual(String s, String t, int flujo, String recurso) {
        CentroView origenView = vistasDeCentros.get(s);
        CentroView destinoView = vistasDeCentros.get(t);

        if (origenView != null && destinoView != null && flujo > 0) {
            Color colorRecurso = recurso.equals("Nutrientes") ? Color.YELLOW : Color.CYAN;
            animarFlujo(origenView, destinoView, flujo, colorRecurso);
        }
    }

    private void animarFlujo(CentroView origenView, CentroView destinoView, int flujoReal, Color color) {
        // Lógica de animación para mostrar el flujo entre nodos
        int numParticulas = Math.min(flujoReal, 10); // Máx 10 para evitar sobrecarga visual

        Line path = new Line(origenView.getCenterX(), origenView.getCenterY(),
                destinoView.getCenterX(), destinoView.getCenterY());

        for (int i = 0; i < numParticulas; i++) {
            FlujoParticle particle = new FlujoParticle(color);
            Duration delay = Duration.millis(i * 100);

            PathTransition pt = new PathTransition(Duration.seconds(1.0), path, particle);
            pt.setDelay(delay);
            pt.setCycleCount(1);

            pt.setOnFinished(e -> root.getChildren().remove(particle));

            root.getChildren().add(particle);
            pt.play();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}