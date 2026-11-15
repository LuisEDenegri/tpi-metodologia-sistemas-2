package pedidos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestor de pedidos de Distribuidora Cuyo.
 *
 * ANTES: 305 lineas, una God Class que hacia todo. El comentario original lo
 * confesaba: nacio como el modulo de inventario, despues se le fue agregando
 * lo de pedidos, despues los clientes, despues el reporte y las
 * notificaciones, y cerraba con una nota pendiente de partirla en algun
 * momento. Este trabajo es ese momento.
 *
 * DESPUES: un coordinador. Recibe la solicitud, valida, le pide a cada
 * colaborador lo suyo y devuelve el resultado. Las reglas de negocio no viven
 * aca: viven en el objeto que las conoce.
 *
 *   Inventario ............ stock
 *   PoliticaDeCostos ...... precios y envio
 *   Pedido ................ su propio ciclo de vida
 *   GeneradorDeReporte .... presentacion
 *   FiltroDePedidos ....... criterios de busqueda
 *   Notificador ........... avisos (abstraccion, no implementacion)
 *
 * SRP: la unica razon para cambiar esta clase es que cambie el flujo de
 * coordinacion. Si cambia una tarifa, se toca PoliticaDeCostos; si cambia el
 * formato del reporte, GeneradorDeReporte. Antes todo eso la tocaba a ella
 * (Divergent Change).
 *
 * DIP: depende de la interfaz Notificador, no de EmailService. Por eso ahora se
 * la puede ejercitar en silencio desde las pruebas.
 */
public class PedidoManager {

    private final Map<String, Pedido> pedidos = new LinkedHashMap<>();
    private final Map<String, Cliente> clientes = new LinkedHashMap<>();
    private final Inventario inventario;
    private final PoliticaDeCostos politica;
    private final Notificador notificador;
    private final GeneradorDeReporte generadorDeReporte = new GeneradorDeReporte();

    private int proximoNumero = 1;

    /** Configuracion de produccion: notifica por mail. */
    public PedidoManager() {
        this(new NotificadorPorEmail());
    }

    /** Permite inyectar el notificador. Lo usan las pruebas. */
    public PedidoManager(Notificador notificador) {
        this(new Inventario(), new PoliticaDeCostos(), notificador);
    }

    public PedidoManager(Inventario inventario, PoliticaDeCostos politica,
                         Notificador notificador) {
        this.inventario = inventario;
        this.politica = politica;
        this.notificador = notificador;
    }

    /**
     * Registra un pedido.
     *
     * ANTES: 87 lineas y 13 parametros. Hacia ocho cosas distintas: daba de
     * alta el cliente, validaba stock, creaba el pedido, buscaba el precio,
     * calculaba el envio, descontaba stock, guardaba y notificaba.
     *
     * DESPUES: el flujo completo se lee en una pantalla y cada paso delega.
     *
     * CORRECCION - ATOMICIDAD: el codigo original daba de alta al cliente ANTES
     * de validar el stock, asi que un pedido rechazado dejaba un cliente
     * persistido que nunca compro nada. Ahora se valida todo primero y recien
     * despues se escribe: o pasan las dos cosas, o no pasa ninguna.
     *
     * @return el numero de pedido, o null si fue rechazado.
     */
    public String registrar(SolicitudDePedido solicitud) {
        if (solicitud == null || !solicitud.esValida()) {
            return null;
        }
        if (!inventario.haySuficiente(solicitud.codigoProducto(), solicitud.cantidad())) {
            return null;
        }

        // A partir de aca no hay vuelta atras: todo lo que sigue debe completarse.
        Cliente cliente = registrarOActualizarCliente(solicitud);
        Pedido pedido = crearPedido(solicitud);

        inventario.descontar(solicitud.codigoProducto(), solicitud.cantidad());
        pedidos.put(pedido.numero(), pedido);

        notificarAltaDePedido(cliente, pedido);
        return pedido.numero();
    }

    /**
     * REFACTORING: Extract Method.
     *
     * CORRECCION: antes, si el cliente ya existia, el bloque entero se saltaba
     * y sus datos quedaban congelados en el primer pedido. Ahora se actualizan
     * con los del pedido mas reciente.
     */
    private Cliente registrarOActualizarCliente(SolicitudDePedido solicitud) {
        Cliente existente = clientes.get(solicitud.documentoCliente());
        if (existente == null) {
            Cliente nuevo = solicitud.comoCliente();
            clientes.put(nuevo.documento(), nuevo);
            return nuevo;
        }
        existente.actualizarDatosDeContacto(solicitud.nombreCliente(), solicitud.email(),
                solicitud.telefono(), solicitud.direccion());
        return existente;
    }

    /** REFACTORING: Extract Method. El calculo de importes delega en la politica. */
    private Pedido crearPedido(SolicitudDePedido solicitud) {
        Dinero subtotal = politica.subtotal(solicitud.codigoProducto(), solicitud.cantidad());
        Dinero envio = politica.costoDeEnvio(solicitud.tipoEnvio(),
                solicitud.direccion().zona(), subtotal, solicitud.esUrgente());
        String numero = "PED-" + proximoNumero;
        proximoNumero++;
        return new Pedido(numero, solicitud, subtotal, envio);
    }

    /** REFACTORING: Extract Method. Notificar no es registrar. */
    private void notificarAltaDePedido(Cliente cliente, Pedido pedido) {
        notificador.notificarAlCliente(cliente, "Pedido " + pedido.numero() + " registrado");
        notificador.registrarEvento("PEDIDO_NUEVO", pedido.numero());

        String codigo = pedido.codigoProducto();
        if (inventario.tieneStockBajo(codigo)) {
            notificador.notificarStockBajo(codigo, inventario.disponible(codigo));
        }
    }

    /**
     * Hace avanzar un pedido a otro estado.
     *
     * ANTES: 45 lineas de if/else anidados que mezclaban tres cosas: validar la
     * transicion, reponer stock y notificar. La reposicion de stock estaba
     * duplicada en dos ramas distintas (Duplicated Code).
     *
     * DESPUES: el pedido valida su propia transicion, el inventario repone y el
     * notificador avisa. Cada uno lo suyo.
     *
     * @return true si la transicion se aplico.
     */
    public boolean cambiarEstado(String numero, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidos.get(numero);
        if (pedido == null) {
            return false;
        }
        if (!pedido.cambiarEstadoA(nuevoEstado)) {
            return false;
        }
        if (nuevoEstado.reponeStock()) {
            inventario.reponer(pedido.codigoProducto(), pedido.cantidad());
        }
        notificador.notificarAlCliente(clientes.get(pedido.documentoCliente()),
                "Pedido " + numero + " ahora esta " + nuevoEstado);
        notificador.registrarEvento("CAMBIO_ESTADO", numero);
        return true;
    }

    /** Acepta el estado como texto. Devuelve false si no corresponde a ninguno. */
    public boolean cambiarEstado(String numero, String nuevoEstado) {
        return cambiarEstado(numero, EstadoPedido.desdeTexto(nuevoEstado));
    }

    public boolean asignarResponsable(String numero, String responsable, String area) {
        Pedido pedido = pedidos.get(numero);
        if (pedido == null) {
            return false;
        }
        pedido.asignarResponsable(responsable, area);
        notificador.registrarEvento("ASIGNACION", numero);
        return true;
    }

    /**
     * CORRECCION DE SEGURIDAD: el filtro recibe los clientes como vista
     * inmutable y no como el mapa interno.
     *
     * El filtro es un punto de extension: cualquiera puede heredar de
     * FiltroDePedidos y redefinir aceptaA. Pasarle el LinkedHashMap vivo
     * significaba que una subclase podia quedarse la referencia y vaciar la
     * base de clientes con un clear(). Una consulta de solo lectura borraba
     * datos. El refactoring introdujo ese punto de extension y hay que
     * protegerlo: quien extiende no tiene por que poder escribir.
     */
    public List<Pedido> buscar(FiltroDePedidos filtro) {
        FiltroDePedidos efectivo = filtro == null ? new FiltroDePedidos() : filtro;
        Map<String, Cliente> soloLectura = Collections.unmodifiableMap(clientes);
        List<Pedido> resultado = new ArrayList<>();
        for (Pedido p : pedidos.values()) {
            if (efectivo.aceptaA(p, soloLectura)) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    public List<Pedido> todos() {
        return new ArrayList<>(pedidos.values());
    }

    public String reporte() {
        return generadorDeReporte.generar(Collections.unmodifiableCollection(pedidos.values()),
                Collections.unmodifiableMap(clientes));
    }

    /**
     * Total efectivamente facturado.
     *
     * CORRECCION: antes el criterio era "todo lo que no esta CANCELADO", asi
     * que un pedido recien ingresado que nadie pago ya figuraba como
     * facturacion. Ahora cuenta desde PAGADO en adelante.
     */
    public Dinero totalFacturado() {
        Dinero total = Dinero.CERO;
        for (Pedido p : pedidos.values()) {
            if (p.cuentaComoFacturado()) {
                total = total.mas(p.total());
            }
        }
        return total;
    }

    /** Suma un ingreso de mercaderia al stock. */
    public void ingresarStock(String codigoProducto, int cantidad) {
        inventario.ingresar(codigoProducto, cantidad);
    }

    /** Fija el stock tras un recuento fisico, descartando el valor anterior. */
    public void fijarStock(String codigoProducto, int cantidad) {
        inventario.fijar(codigoProducto, cantidad);
    }

    public int stockDe(String codigoProducto) {
        return inventario.disponible(codigoProducto);
    }

    public Pedido getPedido(String numero) {
        return pedidos.get(numero);
    }

    public Cliente getCliente(String documento) {
        return clientes.get(documento);
    }

    /**
     * Vistas de solo lectura.
     *
     * CORRECCION: antes los tres mapas eran campos publicos. La sonda de
     * diagnostico demostro que desde afuera se podia poner el stock en -999 y
     * el estado de un pedido en "INVENTADO", sin que nada lo impidiera. Las
     * invariantes del dominio no pueden depender de la buena voluntad del
     * llamador.
     */
    public Map<String, Pedido> pedidos() {
        return Collections.unmodifiableMap(pedidos);
    }

    public Map<String, Cliente> clientes() {
        return Collections.unmodifiableMap(clientes);
    }

    public Map<String, Integer> stock() {
        return inventario.comoMapa();
    }
}
