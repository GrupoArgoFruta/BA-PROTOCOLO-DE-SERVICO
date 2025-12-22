package br.com.argo.protocoloservico.modal;

import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class ModalProtocoloServico {
	public void tratarErro(ContextoAcao ctx, String titulo, String detalhe) {
	    StringBuilder htmlErro = new StringBuilder();
	    htmlErro.append("<!DOCTYPE html>")
	            .append("<html>")
	            .append("<body style='font-family: Arial, sans-serif;'>")
	            
	            // Ícone de Alerta ou X (Usando imagem externa ou emoji unicode grande)
	            .append("<div style='text-align: center; margin-bottom: 15px;'>")
	            .append("   <span style='font-size: 50px; color: #D32F2F;'>&#9888;</span>") // Emoji de Triângulo de Alerta
	            .append("</div>")

	            // Título do Erro
	            .append("<div style='text-align: center; color: #D32F2F; font-size: 18px; font-weight: bold; margin-bottom: 10px;'>")
	            .append(titulo)
	            .append("</div>")

	            // Detalhe do Erro (Caixa cinza claro)
	            .append("<div style='background-color: #fceceb; border: 1px solid #f5c6cb; border-radius: 5px; padding: 15px; color: #721c24; text-align: center;'>")
	            .append(detalhe)
	            .append("</div>")

	            .append("</body>")
	            .append("</html>");

	    // Define a mensagem HTML
	    ctx.setMensagemRetorno(htmlErro.toString());
	}

}
