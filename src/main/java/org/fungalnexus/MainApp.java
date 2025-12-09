package org.fungalnexus;

import javafx.animation.AnimationTimer;
import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.scene.control.Alert.AlertType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private BorderPane root;
    private Map<String, Point2D> coordenadasDeCentros = new HashMap<>();
    private double TILE_SIZE = 50.0;
    private long startTime = 0;
    private boolean isGameOver = false;
    private final long HEALTH_DECAY_START_TIME = 20_000_000_000L;
    private boolean modoConstruccion = false;
    private final double TOLERANCIA_CLIC = 15.0;
    // Componentes del Juego
    private Canvas canvas;
    private GraphicsContext gc;
    private Pane animationPane;
    // Componentes del HUD (para poder actualizarlos)
    private Label statusLabel;
    private ProgressBar healthBar;
    private ProgressBar expBar;

    // Costos y parametros del juego
    private final int COSTO_EXPANSION_EXTRACCION = 40;
    private final int COSTO_MANTENIMIENTO_BASE_NUCLEO = 8;
    private final int COSTO_MANTENIMIENTO_HIFA_BASE = 2;
    private final double PIXELES_POR_UNIDAD_DE_COSTO = 100.0;
    private final int COSTO_MEJORA_HIFA = 10;

    private final int MAX_EXP_CAPACITY = 500;

    private final double LIMITE_CONSTRUCCION_DISTANCIA = 3.0 * TILE_SIZE;
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;

    @Override
    public void start(Stage primaryStage) {


        // --- Contenedores de Layout ---
        root = new BorderPane(); // Contenedor principal

        // Inicialización del Canvas y GraphicsContext
        canvas = new Canvas(WIDTH, HEIGHT); // Ahora estás inicializando el campo de clase
        gc = canvas.getGraphicsContext2D();

        animationPane = new Pane();
        animationPane.setPrefSize(WIDTH, HEIGHT);
        animationPane.setPickOnBounds(false);

        StackPane centerStack = new StackPane();
        centerStack.getChildren().addAll(canvas, animationPane);

        root.setCenter(centerStack);

        // --- Configuración de Paneles ---

        // 1. PANEL SUPERIOR (HUD)
        VBox topPanel = crearPanelHUD();
        root.setTop(topPanel);

        // 2. PANEL LATERAL DERECHO (Herramientas/Botones)
        VBox rightPanel = crearPanelHerramientas();
        root.setRight(rightPanel);

        // --- Configuración de la Escena ---
        Scene scene = new Scene(root);

        inicializarJuegoBase();

        dibujarGrafo();

        iniciarInteraccion(scene);

        primaryStage.setTitle("Fungus Growth Strategy");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Inicia el ciclo de juego
        iniciarGameLoop();
    }

    private VBox crearPanelHUD() {

        // Usaremos un HBox para colocar los elementos de estado en una fila
        HBox statusBar = new HBox(15); // Espaciado de 15 píxeles
        statusBar.setPadding(new Insets(10, 10, 10, 10)); // Padding alrededor del panel
        statusBar.setStyle("-fx-background-color: #333333;"); // Fondo oscuro para contraste

        // 1. Label para el Balance Económico (Línea de texto)
        statusLabel = new Label("Balance: Calculando...");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        // 2. Barra de Salud (Corazón)
        ProgressBar healthBar = new ProgressBar(1.0); // Inicialmente lleno
        healthBar.setPrefWidth(150);
        // (Debes crear un método para actualizar esta barra basado en nucleo.getSalud())

        // 3. Barra de EXP (Rayo)
        ProgressBar expBar = new ProgressBar(0.0); // Inicialmente vacío
        expBar.setPrefWidth(150);
        // (Debes crear un método para actualizar esta barra basado en nucleo.getExpAcumulada())

        statusBar.getChildren().addAll(statusLabel, healthBar, expBar);

        // Configuramos cómo se estiran los elementos, si es necesario
        HBox.setHgrow(statusLabel, Priority.ALWAYS); // Permite que el label ocupe espacio restante

        return new VBox(statusBar);
    }

    private VBox crearPanelHerramientas() {
        VBox toolBox = new VBox(10); // Espaciado de 10 píxeles
        toolBox.setPadding(new Insets(20, 10, 20, 10));
        toolBox.setPrefWidth(100); // Ancho fijo
        toolBox.setStyle("-fx-background-color: #555555;"); // Fondo para diferenciar

        // --- Botón de Construcción (Martillo) ---
        Button buildButton = new Button("🔨 Construir");
        buildButton.setPrefSize(80, 80);
        buildButton.setOnAction(e -> {
            modoConstruccion = !modoConstruccion;
            // La UI debería dar feedback visual aquí (ej., cambiar el estilo del botón)
            System.out.println("Modo Construcción " + (modoConstruccion ? "ACTIVO" : "DESACTIVADO"));
        });

        // --- Botón de Interacción/Mejora (Puño) ---
        Button interactButton = new Button("👊 Mejorar");
        interactButton.setPrefSize(80, 80);
        interactButton.setDisable(true); // Desactivado hasta que implementemos el Tooltip de mejora.

        toolBox.getChildren().addAll(buildButton, interactButton);
        return toolBox;
    }

    private void inicializarJuegoBase() {
        // Definición de las constantes de tamaño de la ventana
        final double MAP_WIDTH = 1000;
        final double MAP_HEIGHT = 700;

        // 1. Inicializar la colonia (la estructura de datos)
        colonia = new ColoniaGrafo();

        // 2. Crear y configurar el Núcleo Central (Único nodo inicial)
        // Usaremos la EXP inicial aquí si la necesitas para el tutorial
        final int EXP_INICIAL = 50;
        Centro nucleo = new Centro("NucleoCentral", "Central");
        nucleo.sumarExp(EXP_INICIAL); // Opcional: Dale EXP de arranque

        colonia.agregarCentro(nucleo);

        // 3. Posicionamiento del Núcleo (usa las dimensiones fijas)
        // Asegúrate de que 'coordenadasDeCentros' sea un campo de clase inicializado.
        coordenadasDeCentros.put("NucleoCentral", new Point2D(MAP_WIDTH / 2, MAP_HEIGHT / 2));

        // 4. Inicializar el tiempo de inicio
        startTime = System.nanoTime();

        // 5. Limpieza de estado antiguo
        // Si usabas gameState, puedes eliminarlo o inicializarlo a un valor 'PLAYING'.
        // gameState = "PLAYING";

        System.out.println("Juego Base Inicializado. EXP Inicial: " + nucleo.getExpAcumulada());
    }

    private Color getColorForCentro(Centro centro) {
        if (centro.getSalud() <= 20) {
            return Color.DARKRED; // Salud baja
        } else if (centro.getTipo().equals("Central")) {
            return Color.PURPLE; // Núcleo
        } else if (centro.getTipo().equals("Extraccion")) {
            return Color.GREEN; // Extracción
        } else {
            return Color.BROWN; // Default
        }
    }

    // Dibujo y visualizacion
    private void dibujarGrafo() {
        // 1. Limpiar y establecer fondo del Canvas
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.web("#402c18")); // Fondo marrón para el mapa
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 2. Dibujar el Grid
        dibujarGrid(); // La lógica de dibujarGrid también DEBE USAR 'gc' ahora.

        // 3. Dibujar Hifas (Usando gc.strokeLine)
        gc.setStroke(Color.SADDLEBROWN);
        for (String origen : colonia.obtenerNombresDeCentros()) {
            for (Hifa hifa : colonia.obtenerHifas(origen)) {
                Point2D coordsOrigen = coordenadasDeCentros.get(origen);
                Point2D coordsDestino = coordenadasDeCentros.get(hifa.getDestinoNombre());

                if (coordsOrigen != null && coordsDestino != null) {
                    // Dibujo de la línea en el Canvas
                    gc.setLineWidth(hifa.getCapacidadMaxima() / 5.0);
                    gc.strokeLine(coordsOrigen.getX(), coordsOrigen.getY(), coordsDestino.getX(), coordsDestino.getY());
                }
            }
        }

        // 4. Dibujar Nodos (Centros) (Usando gc.fillOval)
        for (Centro centro : colonia.obtenerTodosLosCentros()) {
            Point2D coords = coordenadasDeCentros.get(centro.getNombre());
            double radio = centro.getTipo().equals("Central") ? 30 : 20;

            // Determinar el color: (Deberías mover la lógica de color de CentroView aquí)
            Color fillColor = getColorForCentro(centro);

            gc.setFill(fillColor);
            gc.fillOval(coords.getX() - radio, coords.getY() - radio, 2 * radio, 2 * radio);

            // Dibujar borde
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.0);
            gc.strokeOval(coords.getX() - radio, coords.getY() - radio, 2 * radio, 2 * radio);
        }
    }

    private void dibujarGrid() {
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(0.5);

        double canvasW = canvas.getWidth();
        double canvasH = canvas.getHeight();

        // Dibujar líneas verticales
        for (double i = 0; i < canvasW; i += TILE_SIZE) {
            gc.strokeLine(i, 0, i, canvasH);
        }
        // Dibujar líneas horizontales
        for (double i = 0; i < canvasH; i += TILE_SIZE) {
            gc.strokeLine(0, i, canvasW, i);
        }
    }

    public void actualizarHUD() {
        Centro nucleo = colonia.obtenerCentro("NucleoCentral");
        String recurso = "Nutrientes";

        // A. Conexión del Balance Neto (implementado en el paso anterior)
        int balanceNeto = calcularBalanceEconomicoNeto();
        String iconoBalance = (balanceNeto >= 0) ? "🟢" : "🔴";
        String signoBalance = (balanceNeto >= 0) ? "+" : "";

        // B. Métrica de las Barras (Ej., el MAX_EXP_CAPACITY es 500)
        final int MAX_EXP_CAPACITY = 500;

        // C. Formateo del Label de Estado
        String status = String.format(
                "Balance: %s %s%d u/s | Salud: %d/100 | EXP: %d/%d | Nutrientes: %d",
                iconoBalance,
                signoBalance,
                Math.abs(balanceNeto),
                nucleo.getSalud(),
                nucleo.getExpAcumulada(),
                MAX_EXP_CAPACITY,
                nucleo.getInventario().getOrDefault(recurso, 0)
        );
        statusLabel.setText(status);

        // D. Actualización de las Barras (Si creaste las variables de campo para ellas)
        // healthBar.setProgress(nucleo.getSalud() / 100.0);
        // expBar.setProgress(nucleo.getExpAcumulada() / (double)MAX_EXP_CAPACITY);
    }

    private int costoTotalMantenimiento() {
        return colonia.obtenerTodasLasHifas().stream()
                .flatMap(List::stream)
                .mapToInt(Hifa::getCostoMantenimiento)
                .sum() + COSTO_MANTENIMIENTO_BASE_NUCLEO;
    }

    private void iniciarInteraccion(Scene scene) {

        // --- ÚNICO MANEJADOR: Clic en el Canvas (Zona de Juego) ---
        // El 'canvas' ahora es un campo de clase y está disponible aquí.
        canvas.setOnMouseClicked(e -> {

            Point2D clickPoint = new Point2D(e.getX(), e.getY());
            Centro nucleo = colonia.obtenerCentro("NucleoCentral");

            // 1. Lógica de Colocación de un NUEVO Extractor (Activado por el botón)
            if (modoConstruccion) {
                // Se desactiva el modo al construir, para evitar colocaciones accidentales
                modoConstruccion = false;
                construirCentroExtractor(e.getX(), e.getY());

                // Aquí puedes llamar a una función que actualice el estilo del botón
                // para mostrar que el modo construcción está OFF.
                return;
            }

            // 2. Lógica de Mejora de Hifa
            Hifa hifaClickeada = buscarHifaEnCoordenadas(clickPoint);

            if (hifaClickeada != null) {
                // La lógica de verificación de EXP y mejora se mantiene, pero las alertas
                // de terminal deben ser reemplazadas por mensajes en la UI.

                if (nucleo.getExpAcumulada() >= COSTO_MEJORA_HIFA && hifaClickeada.getCapacidadMaxima() == 10) {
                    // Aplicar Mejora
                    nucleo.restarExp(COSTO_MEJORA_HIFA);
                    hifaClickeada.setCapacidadMaxima(14);

                    // Reemplazar System.out por actualización de UI/log en pantalla
                    // Por ejemplo: mostrarMensajeUI("Hifa mejorada!");
                    System.out.println("Hifa mejorada.");

                    dibujarGrafo();
                    return;
                } else if (hifaClickeada.getCapacidadMaxima() > 10) {
                    // Reemplazar System.out por actualización de UI/log en pantalla
                    System.out.println("Esta Hifa ya está mejorada.");
                } else {
                    // Reemplazar System.err por actualización de UI/log en pantalla
                    System.err.println("EXP insuficiente.");
                }
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
    private void iniciarGameLoop() {
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
                dibujarGrafo();
                actualizarHUD();
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
                    final int MAX_EXP_CAPACITY = 500; // Usar la constante que definiste
                    int expActual = nucleo.getExpAcumulada();
                    int espacioDisponible = MAX_EXP_CAPACITY - expActual;

                    int expGenerada = superavitParaGastar * 1;

                    if (espacioDisponible > 0) {
                        // Solo se acumula la EXP hasta el límite (CAP)
                        int expRealAcumulada = Math.min(expGenerada, espacioDisponible);

                        nucleo.sumarExp(expRealAcumulada);
                        System.out.println("Ganados X EXP...");

                    } else {
                        // ADVERTENCIA: El jugador está en el límite y está desperdiciando recursos.
                        System.out.println("ADVERTENCIA: Límite de EXP alcanzado.");
                    }
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
        // 1. Obtener las coordenadas del centro
        Point2D coordOrigen = coordenadasDeCentros.get(s);
        Point2D coordDestino = coordenadasDeCentros.get(t);

        if (coordOrigen != null && coordDestino != null && flujo > 0) {
            Color colorRecurso = recurso.equals("Nutrientes") ? Color.YELLOW : Color.CYAN;
            // 2. Pasar las coordenadas directamente a la animación
            animarFlujo(coordOrigen, coordDestino, flujo, colorRecurso);
        }
    }

    private void animarFlujo(Point2D coordOrigen, Point2D coordDestino, int flujoReal, Color color) {
        // Lógica de animación para mostrar el flujo entre nodos
        int numParticulas = Math.min(flujoReal, 10);

        // El Path usa las coordenadas de los centros
        Line path = new Line(coordOrigen.getX(), coordOrigen.getY(),
                coordDestino.getX(), coordDestino.getY());

        for (int i = 0; i < numParticulas; i++) {
            FlujoParticle particle = new FlujoParticle(color);
            Duration delay = Duration.millis(i * 100);

            double radius = particle.getRadius();
            particle.setTranslateX(-radius);
            particle.setTranslateY(-radius);

            PathTransition pt = new PathTransition(Duration.seconds(1.0), path, particle);
            pt.setDelay(delay);
            pt.setCycleCount(1);

            // La partícula se remueve del Pane de Animación al terminar
            pt.setOnFinished(e -> animationPane.getChildren().remove(particle));

            // La partícula se añade al Pane de Animación, no al root.
            animationPane.getChildren().add(particle);
            pt.play();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}