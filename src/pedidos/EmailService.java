package pedidos;

/**
 * Envio de mails. Por ahora imprime, cuando tengamos el SMTP se cambia aca.
 */
public class EmailService {

    public void enviar(String destino, String mensaje) {
        System.out.println("[MAIL -> " + destino + "] " + mensaje);
    }
}
