package com.devzone.madrid15;

import static org.odata4j.examples.JaxRsImplementation.JERSEY;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gdata.client.spreadsheet.*;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.*;

import org.core4j.Enumerable;
import org.core4j.Func;
import org.core4j.Funcs;
import org.odata4j.examples.ODataServerFactory;
import org.odata4j.producer.inmemory.InMemoryProducer;
import org.odata4j.producer.resources.DefaultODataProducerProvider;
import org.odata4j.producer.server.ODataServer;

/**
 * Hello world!
 *
 */
public class App 
{
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
	
	public static void main(String[] args)
    		throws AuthenticationException, MalformedURLException, IOException, ServiceException, ParseException, java.text.ParseException
    {
		
		String USERNAME = null;
		String PASSWORD = null;
		
		Map<String, String> env = System.getenv();
		for (String envName : env.keySet())
        {
        	if(envName.equals("GOOGLE_USER"))
        	{
        		USERNAME = env.get(envName);
        	}
        	
        	if(envName.equals("GOOGLE_PASS"))
        	{
        		PASSWORD = env.get(envName);
        	}
        }
		
		String webPort = System.getenv("PORT");
    	if(webPort == null || webPort.isEmpty())
    	{
    		webPort = "8080";
    	}	  
    	String endpointUri = "http://localhost:"+webPort+"/GoogleSheetReader.svc/";


      // InMemoryProducer is a readonly odata provider that serves up POJOs as entities using bean properties
      // call InMemoryProducer.register to declare a new entity-set, providing a entity source function and a propertyname to serve as the key
      final InMemoryProducer producer = new InMemoryProducer("GoogleSheetReader", 100);

      //Create the Serivce
	  SpreadsheetService googleService = new SpreadsheetService("Gastos2015");
	  googleService.setUserCredentials(USERNAME, PASSWORD);
	  
	  //Read SpreadSheet abd expose a large list of stings
 	  // The url will be: http://localhost:8080/GoogleSheetReader.svc/Gastos
 	  	  
	  final SpreadsheetService googleServicefinal = googleService;
	  
	  producer.register(InformationGastos.class,
	  			"Gastos",
	  			new Func<Iterable<InformationGastos>>()
	  			{
					public Iterable<InformationGastos> apply()
					{
						try
						{
							return readGoogleSpreadhSheet(googleServicefinal);
						}
						catch (java.text.ParseException e)
						{
							return null;
						}
					}
	  			},
	  			"GastoId");
	  
	  DefaultODataProducerProvider.setInstance(producer);
	  ODataServer server = new ODataServerFactory(JERSEY).startODataServer(endpointUri);
  }
  
  public static List<InformationGastos> readGoogleSpreadhSheet(SpreadsheetService googleService) throws java.text.ParseException
  {
  	List<InformationGastos> gastosInfo = new ArrayList<InformationGastos>();
  	
  	URL feedUrl = null;
  	try
  	{
  		feedUrl = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full");
  	}
  	catch(MalformedURLException e)
  	{
  		//TODO Auto-generated catch block
  		e.printStackTrace();
  	}

  	SpreadsheetFeed feed;
  	try
  	{
  		feed = googleService.getFeed(feedUrl, SpreadsheetFeed.class);
  		List<SpreadsheetEntry> spreadsheets = feed.getEntries();
  		  
  		if(spreadsheets != null)
  		{
  			//Make a request to the API to fetch information about all
  			//worksheets in the spreadsheet.

  			//Retrieve the SpreadhSheetEntry --> the only one
  			SpreadsheetEntry spreadsheet = spreadsheets.get(0);
  			List<WorksheetEntry> worksheets = spreadsheet.getWorksheets();

  			//Iterate through each worksheet in the spreadsheet.
  			for (WorksheetEntry worksheet : worksheets)
  			{
  				//Fetch column 4, and every row after row 1.
  				URL listFeedUrl = worksheet.getListFeedUrl();
  				ListFeed listFeed = googleService.getFeed(listFeedUrl, ListFeed.class);

  				//Iterate through each row, printing its cell values.
  				int counter = 1;
  				for(ListEntry row : listFeed.getEntries())
  				{
  					//Iterate over the remaining columns, and print each cell value
  					InformationGastos gasto = new InformationGastos();
  					for (String tag : row.getCustomElements().getTags())
  					{
  						//Get the value of those cells which column header name is the tag one.
  						//System.out.println("hola Tag value = " + tag);
  						  
  						if(row.getCustomElements().getValue(tag) != null)
  						{
  							if(tag == "identificador")
  								gasto.gastoId= Integer.parseInt(row.getCustomElements().getValue(tag));
  							if(tag == "pagado")
  								gasto.pagado = row.getCustomElements().getValue(tag).equals("Si") ? true : false;
  							if(tag == "fechainicio")
  								gasto.fechaInicio = getDate(row.getCustomElements().getValue(tag));
  							if(tag == "fechafin")
  								gasto.fechaFin = getDate(row.getCustomElements().getValue(tag));
  							if(tag == "concepto")
  								gasto.concepto = row.getCustomElements().getValue(tag);
  							if(tag == "importe")
  								gasto.importe = Double.parseDouble(row.getCustomElements().getValue(tag));    							  
  						}						  						  
  					}
  					gastosInfo.add(gasto);
  					counter++;
  				}
  			}
  		}
  	}
  	catch (IOException e1)
  	{
  		// TODO Auto-generated catch block
  		e1.printStackTrace();
  	}
  	catch (ServiceException e1)
  	{
  		//TODO Auto-generated catch block
  		e1.printStackTrace();
  	}
  	
  	return gastosInfo;    	
  }
  
  public static class InformationGastos
  {
  	private int gastoId;
  	private Date fechaInicio;
  	private Date fechaFin;
  	private String concepto;
		private double importe;
		private boolean pagado;

		public InformationGastos(){}
		
		public InformationGastos(int gId, Date gfInicio, Date gfFin, String gConcepto, double gImporte, boolean gPagado)
		{
			gastoId = gId;
			fechaInicio = gfInicio;
			fechaFin = gfFin;
			concepto = gConcepto;
			importe = gImporte;
			pagado = gPagado;
		}
		
		public int getGastoId()
		{
			return gastoId;			
		}
		
		public void setGastoId(int value)
		{
			gastoId = value;
		}
		
		public Date getFechaInicio()
		{
			return fechaInicio;
		}
		
		public void setFechaInicio(Date value)
		{
			fechaInicio = value;
		}
		
		public Date getFechaFin()
		{
			return fechaFin;
		}
		
		public void setFechaFin(Date value)
		{
			fechaFin = value;
		}
		
		public String getConcepto()
		{
			return concepto;
		}
		
		public void setConcepto(String value)
		{
			concepto = value;
		}
		
		public double getImporte()
		{
			return importe;
		}
		
		public void setImporte(double value)
		{
			importe = value;
		}
		
		public boolean getPagado()
		{
			return pagado;
		}
		
		public void setPagado(boolean value)
		{
			pagado = value;
		}
  }
  
  private static Date getDate(String dateAsString) throws ParseException
  {
    try
    {
    	return DATE_FORMAT.parse(dateAsString);
    }
    catch (java.text.ParseException e)
    {
    	//TODO Auto-generated catch block
    	e.printStackTrace();
    	return null;
    }
  }
}
