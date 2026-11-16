package pedidos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Notificador de produccion: manda mail y registra analitica.
 *
 * CORRECCION DE DEFECTO. El codigo original hacia esto:
 *
 *     try {
 *         Cliente c = clientes.get(p.doc);
 *         emailService.enviar(c.email, ...);
 *         analytics.registrar(...);
 *     } catch (Exception e) {
 *     }
 *
 * Un catch vacio sobre Exception. Si el cliente no estaba en el mapa, c.email
 * lanzaba NullPointerException, el catch se la tragaba entera y cambiarEstado
 * devolvia true igual. El llamador creia que todo habia salido bien.
 *
 * Mientras EmailService solo imprime por consola el problema es invisible. El
 * dia que haya un SMTP real, el sistema puede dejar de notificar a todos los
 * clientes y nadie se entera hasta que llame el primero.
 *
 * Criterio aplicado: una notificacion que falla NO debe cancelar la operacion
 * de negocio (el pedido se pago igual), pero TAMPOCO debe desaparecer sin
 * dejar rastro. Las fallas se registran y quedan consultables.
 */
public class NotificadorPorEmail implements Notificador {

    private static final String MAIL_DE_DEPOSITO = "deposito@distribuidoracuyo.com";

    private final EmailService emailService;
    private final AnalyticsService analytics;
    private final List<String> fallas = new ArrayList<>();

    public NotificadorPorEmail() {
        this(new EmailService(), new AnalyticsService());
    }

    public NotificadorPorEmail(EmailService emailService, AnalyticsService analytics) {
        this.emailService = emailService;
        this.analytics = analytics;
    }

    @Override
    public void notificarAlCliente(Cliente cliente, String mensaje) {
        if (cliente == null) {
            registrarFalla("No se pudo notificar: cliente inexistente. Mensaje: " + mensaje);
            return;
        }
        if (cliente.email() == null || cliente.email().trim().isEmpty()) {
            registrarFalla("No se pudo notificar a " + cliente.documento()
                    + ": sin direccion de mail. Mensaje: " + mensaje);
            return;
        }
        intentar(() -> emailService.enviar(cliente.email(), mensaje),
                "mail a " + cliente.email());
    }

    /**
     * Avisa al deposito y nada mas.
     *
     * LSP: antes este metodo registraba ademas el evento STOCK_BAJO por su
     * cuenta, cosa que NotificadorSilencioso no hacia. Las dos implementaciones
     * de la misma interfaz tenian comportamiento observable distinto, asi que
     * sustituir una por otra cambiaba el resultado: el evento existia en
     * produccion y era invisible desde las pruebas, que es justo lo que el
     * notificador de pruebas viene a evitar.
     *
     * La interfaz nunca dijo si avisar implica registrar un evento, y cada
     * implementacion contesto distinto. La decision pasa al coordinador, que es
     * donde estan los demas registrarEvento y donde se puede leer completa.
     */
    @Override
    public void notificarStockBajo(String codigoProducto, int disponible) {
        intentar(() -> emailService.enviar(MAIL_DE_DEPOSITO,
                        "Stock bajo de " + codigoProducto + ": quedan " + disponible),
                "aviso de stock bajo de " + codigoProducto);
    }

    @Override
    public void registrarEvento(String evento, String referencia) {
        intentar(() -> analytics.registrar(evento, referencia),
                "evento " + evento + " de " + referencia);
    }

    /**
     * Ejecuta un aviso sin dejar que su falla voltee la operacion de negocio.
     * A diferencia del catch vacio original, la falla queda registrada.
     */
    private void intentar(Runnable aviso, String descripcion) {
        try {
            aviso.run();
        } catch (RuntimeException e) {
            registrarFalla("Fallo el envio de " + descripcion + ": " + e);
        }
    }

    private void registrarFalla(String detalle) {
        fallas.add(detalle);
        System.err.println("[AVISO NO ENVIADO] " + detalle);
    }

    /** Fallas acumuladas. Permite verificar que los avisos salieron. */
    public List<String> fallas() {
        return Collections.unmodifiableList(fallas);
    }

    public boolean huboFallas() {
        return !fallas.isEmpty();
    }
}
