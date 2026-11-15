package pedidos;

/**
 * Avisos que el sistema emite hacia afuera.
 *
 * REFACTORING: Extract Interface + Dependency Inversion (SOLID, principio D).
 *
 * Antes PedidoManager instanciaba directamente EmailService y AnalyticsService
 * con `new`. Eso lo ataba a implementaciones concretas: no habia forma de
 * ejecutar la logica de pedidos sin que escribiera por consola, y por eso la
 * salida de las pruebas venia mezclada con notificaciones.
 *
 * Con la dependencia invertida, el gestor depende de esta abstraccion y quien
 * lo construye decide que implementacion usar: la real en produccion, una
 * silenciosa en las pruebas. El modulo de alto nivel deja de depender del
 * detalle.
 */
public interface Notificador {

    /** Avisa al cliente algo sobre su pedido. */
    void notificarAlCliente(Cliente cliente, String mensaje);

    /** Avisa a deposito que un producto esta por agotarse. */
    void notificarStockBajo(String codigoProducto, int disponible);

    /** Registra un evento de negocio para analitica. */
    void registrarEvento(String evento, String referencia);
}
