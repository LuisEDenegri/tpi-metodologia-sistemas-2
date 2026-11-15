package pedidos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestor de pedidos de Distribuidora Cuyo.
 *
 * Esta clase nacio como el modulo de inventario del sistema. Despues se le fue
 * agregando lo de pedidos, despues los clientes, despues el reporte para
 * administracion y las notificaciones. Funciona.
 *
 * TODO: habria que partirla en algun momento. Hablarlo con el equipo.
 * TODO: lo del calculo de envio esta duplicado abajo, unificar cuando haya tiempo.
 */
public class PedidoManager {

    // los pedidos, por numero
    public Map<String, Pedido> pedidos = new HashMap<>();

    // stock por codigo de producto - viene del modulo viejo de inventario
    public Map<String, Integer> stock = new HashMap<>();

    // clientes por documento
    public Map<String, Cliente> clientes = new HashMap<>();

    private int contador = 1;

    private EmailService emailService = new EmailService();
    private AnalyticsService analytics = new AnalyticsService();

    public String registrarPedido(String doc, String nombre, String email, String telefono,
                                  String calle, String ciudad, String provincia, String cp,
                                  String codigoProducto, int cantidad, String tipoEnvio,
                                  String observaciones, boolean urgente) {

        Cliente c = clientes.get(doc);
        if (c == null) {
            c = new Cliente();
            c.doc = doc;
            c.nombre = nombre;
            c.email = email;
            c.telefono = telefono;
            c.calle = calle;
            c.ciudad = ciudad;
            c.provincia = provincia;
            c.cp = cp;
            clientes.put(doc, c);
        }

        Integer disponible = stock.get(codigoProducto);
        if (disponible == null) {
            disponible = 0;
        }
        if (disponible < cantidad) {
            return null;
        }

        Pedido p = new Pedido();
        p.numero = "PED-" + contador;
        contador = contador + 1;
        p.doc = doc;
        p.codigoProducto = codigoProducto;
        p.cantidad = cantidad;
        p.tipoEnvio = tipoEnvio;
        p.observaciones = observaciones;
        p.urgente = urgente;
        p.estado = "PENDIENTE";

        double precio = 0;
        if (codigoProducto.startsWith("VIN")) {
            precio = 8500;
        } else if (codigoProducto.startsWith("ACE")) {
            precio = 6200;
        } else if (codigoProducto.startsWith("DUL")) {
            precio = 3400;
        } else if (codigoProducto.startsWith("FRU")) {
            precio = 4800;
        } else {
            precio = 5000;
        }
        p.subtotal = precio * cantidad;

        // calculo del envio
        double envio = 0;
        if (tipoEnvio.equals("RETIRO")) {
            envio = 0;
        } else if (provincia.equals("Mendoza")) {
            envio = 1200;
        } else if (provincia.equals("CABA") || provincia.equals("Buenos Aires")) {
            envio = 2800;
        } else {
            envio = 4500;
        }
        if (urgente) {
            envio = envio * 1.5;
        }
        if (p.subtotal > 25000) {
            envio = 0;
        }
        p.envio = envio;
        p.total = p.subtotal + envio;

        stock.put(codigoProducto, disponible - cantidad);

        pedidos.put(p.numero, p);

        // avisar
        emailService.enviar(c.email, "Pedido " + p.numero + " registrado");
        analytics.registrar("PEDIDO_NUEVO", p.numero);
        if (stock.get(codigoProducto) < 10) {
            emailService.enviar("deposito@distribuidoracuyo.com",
                    "Stock bajo de " + codigoProducto);
            analytics.registrar("STOCK_BAJO", codigoProducto);
        }

        return p.numero;
    }

    public boolean cambiarEstado(String numero, String nuevoEstado) {
        Pedido p = pedidos.get(numero);
        if (p == null) {
            return false;
        }

        if (p.estado.equals("PENDIENTE")) {
            if (nuevoEstado.equals("PAGADO")) {
                p.estado = nuevoEstado;
            } else if (nuevoEstado.equals("CANCELADO")) {
                p.estado = nuevoEstado;
                Integer d = stock.get(p.codigoProducto);
                if (d == null) {
                    d = 0;
                }
                stock.put(p.codigoProducto, d + p.cantidad);
            } else {
                return false;
            }
        } else if (p.estado.equals("PAGADO")) {
            if (nuevoEstado.equals("PREPARACION")) {
                p.estado = nuevoEstado;
            } else if (nuevoEstado.equals("CANCELADO")) {
                p.estado = nuevoEstado;
                Integer d = stock.get(p.codigoProducto);
                if (d == null) {
                    d = 0;
                }
                stock.put(p.codigoProducto, d + p.cantidad);
            } else {
                return false;
            }
        } else if (p.estado.equals("PREPARACION")) {
            if (nuevoEstado.equals("DESPACHADO")) {
                p.estado = nuevoEstado;
            } else {
                return false;
            }
        } else if (p.estado.equals("DESPACHADO")) {
            if (nuevoEstado.equals("ENTREGADO")) {
                p.estado = nuevoEstado;
            } else {
                return false;
            }
        } else {
            return false;
        }

        try {
            Cliente c = clientes.get(p.doc);
            emailService.enviar(c.email, "Pedido " + numero + " ahora esta " + nuevoEstado);
            analytics.registrar("CAMBIO_ESTADO", numero);
        } catch (Exception e) {
        }

        return true;
    }

    public boolean asignarResponsable(String numero, String responsable, String area) {
        Pedido p = pedidos.get(numero);
        if (p == null) {
            return false;
        }
        p.responsable = responsable;
        p.area = area;
        analytics.registrar("ASIGNACION", numero);
        return true;
    }

    public List<Pedido> buscar(String estado, String provincia, Double montoMinimo,
                               Boolean soloUrgentes, String codigoProducto) {
        List<Pedido> resultado = new ArrayList<>();
        for (String k : pedidos.keySet()) {
            Pedido p = pedidos.get(k);
            boolean pasa = true;
            if (estado != null && !p.estado.equals(estado)) {
                pasa = false;
            }
            if (provincia != null) {
                Cliente c = clientes.get(p.doc);
                if (c == null || !c.provincia.equals(provincia)) {
                    pasa = false;
                }
            }
            if (montoMinimo != null && p.total < montoMinimo) {
                pasa = false;
            }
            if (soloUrgentes != null && soloUrgentes && !p.urgente) {
                pasa = false;
            }
            if (codigoProducto != null && !p.codigoProducto.equals(codigoProducto)) {
                pasa = false;
            }
            if (pasa) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    public String reporte() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE DE PEDIDOS ===\n");

        double totalGeneral = 0;
        int cantPendientes = 0;
        int cantPagados = 0;
        int cantPreparacion = 0;
        int cantDespachados = 0;
        int cantEntregados = 0;
        int cantCancelados = 0;

        for (String k : pedidos.keySet()) {
            Pedido p = pedidos.get(k);

            if (!p.estado.equals("CANCELADO")) {
                totalGeneral = totalGeneral + p.total;
            }

            if (p.estado.equals("PENDIENTE")) {
                cantPendientes++;
            } else if (p.estado.equals("PAGADO")) {
                cantPagados++;
            } else if (p.estado.equals("PREPARACION")) {
                cantPreparacion++;
            } else if (p.estado.equals("DESPACHADO")) {
                cantDespachados++;
            } else if (p.estado.equals("ENTREGADO")) {
                cantEntregados++;
            } else if (p.estado.equals("CANCELADO")) {
                cantCancelados++;
            }

            Cliente c = clientes.get(p.doc);
            String nom = "";
            if (c != null) {
                nom = c.nombre;
            }
            sb.append(p.numero + " | " + nom + " | " + p.codigoProducto
                    + " x" + p.cantidad + " | " + p.estado + " | $" + p.total);
            if (p.urgente) {
                sb.append(" | URGENTE");
            }
            sb.append("\n");
        }

        sb.append("--- Pendientes: " + cantPendientes + "\n");
        sb.append("--- Pagados: " + cantPagados + "\n");
        sb.append("--- En preparacion: " + cantPreparacion + "\n");
        sb.append("--- Despachados: " + cantDespachados + "\n");
        sb.append("--- Entregados: " + cantEntregados + "\n");
        sb.append("--- Cancelados: " + cantCancelados + "\n");
        sb.append("--- Facturado: $" + totalGeneral + "\n");

        return sb.toString();
    }

    public double totalFacturado() {
        double t = 0;
        for (String k : pedidos.keySet()) {
            Pedido p = pedidos.get(k);
            if (!p.estado.equals("CANCELADO")) {
                t = t + p.total;
            }
        }
        return t;
    }

    public void cargarStock(String codigoProducto, int cantidad) {
        stock.put(codigoProducto, cantidad);
    }

    public int stockDe(String codigoProducto) {
        Integer d = stock.get(codigoProducto);
        if (d == null) {
            return 0;
        }
        return d;
    }

    public Pedido getPedido(String numero) {
        return pedidos.get(numero);
    }
}
