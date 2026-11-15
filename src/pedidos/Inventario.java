package pedidos;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stock disponible por producto.
 *
 * REFACTORING: Extract Class (Fowler).
 *
 * El comentario del codigo original decia que PedidoManager "nacio como el
 * modulo de inventario". Eso era precisamente el problema: el inventario nunca
 * se fue, quedo mezclado con pedidos, clientes, reportes y notificaciones en
 * una sola clase. Extraerlo le devuelve una responsabilidad unica a cada parte.
 *
 * La extraccion tambien corrige un defecto que el nombre del metodo original
 * escondia: cargarStock(codigo, 20) REEMPLAZABA el stock en vez de sumarlo.
 * Quien recibia 20 cajas mas y llamaba cargarStock(20) acababa de borrar las 50
 * que ya tenia. Aca la operacion se parte en dos, con nombres que dicen lo que
 * hacen:
 *
 *   - ingresar(codigo, cantidad): suma un ingreso de mercaderia.
 *   - fijar(codigo, cantidad):    fija el valor tras un recuento fisico.
 *
 * ponytail: el stock es un entero por codigo, igual que en el sistema original.
 * No se modela reserva de stock, lotes ni vencimientos. Agregarlo cuando el
 * negocio lo pida: la extraccion deja el cambio contenido en esta clase.
 */
public class Inventario {

    /** Umbral por debajo del cual se avisa a deposito. */
    public static final int UMBRAL_DE_STOCK_BAJO = 10;

    private final Map<String, Integer> stockPorProducto = new LinkedHashMap<>();

    /** Suma un ingreso de mercaderia al stock existente. */
    public void ingresar(String codigoProducto, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "El ingreso debe ser positivo, se recibio " + cantidad);
        }
        stockPorProducto.put(codigoProducto, disponible(codigoProducto) + cantidad);
    }

    /**
     * Fija el stock al resultado de un recuento fisico, descartando el valor
     * anterior. Es lo que hacia cargarStock, pero ahora el nombre lo dice.
     */
    public void fijar(String codigoProducto, int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException(
                    "El stock no puede ser negativo, se recibio " + cantidad);
        }
        stockPorProducto.put(codigoProducto, cantidad);
    }

    public int disponible(String codigoProducto) {
        return stockPorProducto.getOrDefault(codigoProducto, 0);
    }

    public boolean haySuficiente(String codigoProducto, int cantidad) {
        return cantidad > 0 && disponible(codigoProducto) >= cantidad;
    }

    /**
     * Descuenta mercaderia comprometida en un pedido.
     *
     * Falla ruidosamente si no hay stock: antes, con una cantidad negativa, el
     * descuento restaba un negativo e INFLABA el inventario en silencio.
     */
    public void descontar(String codigoProducto, int cantidad) {
        if (!haySuficiente(codigoProducto, cantidad)) {
            throw new IllegalStateException("Stock insuficiente de " + codigoProducto
                    + ": hay " + disponible(codigoProducto) + ", se piden " + cantidad);
        }
        stockPorProducto.put(codigoProducto, disponible(codigoProducto) - cantidad);
    }

    /** Devuelve al stock la mercaderia de un pedido cancelado. */
    public void reponer(String codigoProducto, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La reposicion debe ser positiva, se recibio " + cantidad);
        }
        stockPorProducto.put(codigoProducto, disponible(codigoProducto) + cantidad);
    }

    public boolean tieneStockBajo(String codigoProducto) {
        return disponible(codigoProducto) < UMBRAL_DE_STOCK_BAJO;
    }

    /**
     * Vista de solo lectura. El mapa interno no se expone.
     *
     * Es copia Y es inmutable, y las dos cosas hacen falta por motivos
     * distintos: la copia evita que una escritura externa toque el inventario
     * real, y el envoltorio inmutable hace que esa escritura FALLE en vez de
     * aceptarse en silencio sobre una copia que nadie va a leer. Antes devolvia
     * solo la copia: quien hacia put() creia haber cambiado el stock y se
     * enteraba tarde. Ademas unifica el contrato con pedidos() y clientes(),
     * que ya devolvian vistas inmutables.
     */
    public Map<String, Integer> comoMapa() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(stockPorProducto));
    }
}
