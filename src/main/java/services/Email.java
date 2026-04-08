package services;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.Properties;

// Servicio de envío de correos electrónicos via SMTP (Gmail compatible).
// Soporta enviar texto plano y también adjuntar archivos PDF.
public class Email {

    private final String remitenteEmail;
    private final String remitentePassword;

    public Email(String remitenteEmail, String remitentePassword) {
        this.remitenteEmail = remitenteEmail;
        this.remitentePassword = remitentePassword;
    }

    // Envía un email con o sin adjunto. Si adjunto es null, solo envía el texto.
    public boolean enviarEmailConAdjunto(String destinatarioEmail, String asunto, String cuerpoMensaje, File archivoAdjunto) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(remitenteEmail, remitentePassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(remitenteEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatarioEmail));
            message.setSubject(asunto);

            if (archivoAdjunto != null && archivoAdjunto.exists()) {
                // Mensaje con adjunto
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(cuerpoMensaje);

                MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(archivoAdjunto);
                attachmentBodyPart.setDataHandler(new DataHandler(source));
                attachmentBodyPart.setFileName(archivoAdjunto.getName());

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);
                multipart.addBodyPart(attachmentBodyPart);
                message.setContent(multipart);
            } else {
                // Solo texto (prueba de conexión)
                message.setText(cuerpoMensaje);
            }

            Transport.send(message);
            return true;

        } catch (MessagingException e) {
            System.err.println("Error enviando correo: " + e.getMessage());
            return false;
        }
    }
}