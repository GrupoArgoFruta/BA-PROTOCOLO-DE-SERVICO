package br.com.argo.protocoloservico.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class HistoricoProtocoloPrincipal{

	public void lancarHistProtocolo(BigDecimal nUnico,BigDecimal nUmnota,String infoProtocolo,
			String nomeparc,String nomeEmpre,BigDecimal codEmpresa,String DescriCust,
			String descriNatureza, Timestamp dataenvio,String existeRateio,String datasVencimento, BigDecimal vlrTotal, String ultimadescricao ) throws MGEModelException {
		// TODO Auto-generated method stub
		
		JapeSession.SessionHandle hnd = null;
		JapeWrapper hisDAO = JapeFactory.dao("AD_THPS");
		try {
			
			hnd = JapeSession.open(); // Abertura da sessão do JapeSession
			DynamicVO histoVo = hisDAO.create()
				.set("NUNOTA", nUnico)
				.set("NUMNOTA", nUmnota)
				.set("AD_PROTSERVICO", infoProtocolo)
				.set("NOMEPARC", nomeparc)
				.set("NOMEFANTASIA", nomeEmpre)
				.set("CODEMP", codEmpresa)
				.set("DESCRCENCUS", DescriCust)
				.set("DESCRNAT", descriNatureza)
				.set("AD_DTPROCOSERVI", dataenvio)
				.set("RATEIO", existeRateio)
				.set("DTVENC", datasVencimento)
				.set("VLRNOTA", vlrTotal)
				.set("STATUS", "N")
				.set("AD_NUMANEXO", ultimadescricao)
				
				.save();  	
			
		} catch (Exception e) {
			MGEModelException.throwMe(e);
		} finally {
			JapeSession.close(hnd);
		}

	}

}
