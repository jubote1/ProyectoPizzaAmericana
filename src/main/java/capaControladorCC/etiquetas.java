package capaControladorCC;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.standard.PrinterName;
public class etiquetas {

   public static void main(String[] args) {
       
       try {
           
           PrintService psZebra = null;
           String sPrinterName = null;
           PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
           
           for (int i = 0; i < services.length; i++) {
               
               PrintServiceAttribute attr = services[i].getAttribute(PrinterName.class);
               sPrinterName = ((PrinterName) attr).getValue();
               
               if (sPrinterName.indexOf("Etiqueteadora") >= 0) {
                   psZebra = services[i];
                   break;
               }
           }
           
           if (psZebra == null) {
               System.out.println("Zebra printer is not found.");
               return;
           }
           DocPrintJob job = psZebra.createPrintJob();

           
           //String s = "^XA^FO100,40^BY3^B3, 30^FD123ABC, ^XZ";
           String s = "^XA~TA000~JSN^LT0^MNW^MTT^PON^PMN^LH0,0^JMA^PR4,4~SD15^JUS^LRN^CI28^XZ\r\n" + 
           		"^XA\r\n" + 
           		"^MMT\r\n" + 
           		"^PW799\r\n" + 
           		"^LL0639\r\n" + 
           		"^LS0\r\n" + 
           		"\r\n" + 
           		"^FO30,32^GFA,02560,02560,00020,:Z64:\r\n" + 
           		"eJztlM9rG0cUx9+MrexmcSsbEpqC0lkMBTG3FAIuFEuGNG7pxSFx6aW4h6anHnJoKQXZO95Llb2UQEsJFLzd0zCn/gdd4ZilUFAOhbYQkEwPOnaFFSSD5OmbXf2y+xek5IFA+vLRm3nvfd8A/O/iqtZ6sHtOcpRSEpxwXvO0PhtC4cE5TFKlAOY44qUD0mNA0jlMIiG5oHOcl0IKNa9dnHFUusiVeYj8FIvXoP3A03FxMNUUlJHjSsw41oPKfgwVnRSHE437wIXhgmDCkbNLyLWBNQ/6zyanKppxTqCi+rRUq+3tD0os0XrCSUpD7kvbNhnH6VKy0PbItVIJvO7iuFYByIFtO6Amx3pASNuD0pJFRkMyLkJg2yjYjkKc5un0foO0LSh4mqTrVp6uLjAo4DzKUuWcdxg3GjHBeZi59cfFZpzKgqs8HYlJIzbYzor1XAtTbBAKKgQSkgIVjtEs9k9sxTsxS467u6fMig3nqNARyg/Al1zyyHAx020v0c0fazvDSs3rIkcFVyFXqk6VwzkvS+SsuKJTPHTjrmZ763u1XeQcyDjp+EHADYhGKAFrpYhuJq0DxipXam0AFx0VKuX7fiC4w6lE7guwOje0ZknnCdGMFYdoQB5i05QIoiikgRM43MWuHOP1hiXW/7tttYoHrJJxSiipHD8K8eQx19XgnWK+k37SbCJpuEiZD3JRFJgoo6X6ult6rXXwuNs91Ekz0WvZWkQRZqqHyjc9CWzkdKvfSZrf/JXqRg9gGQsDO/Ijx1e0LnkdS1cS85U2D3vFZGj9OqhcZ2Z0aFQbPYU1+HVpqGznYMk6bJx0+slvz48Q+tLLOKwxMoEDFpktzNye7Xa7J4d/eHq2VVTKMIx8qmYbZLFebdTtP90ZZT+3Moz7ZRmqugpnf7Uqy7XRQLORqWHClctcSnQAfsWf2dELp8O92inRVYBVC6zljIsUDt+8GIZz3YzrnK3XvtZPYQXuFODd/HoRDt/N3Omi93PXJxW2tzkE8m315uLRnWomBsZP2XEKVv2ca/3C3vEKACvEvr3yZ5xz3MFXQ+DNbAjd7A9w1GLseBM2yMbd7Ve28+JC9J0wN7NXhe3m2nFzf3TvU3i08dmVny5fyrUoENK8VpRCaI+3/KvmD+nWUgGqH97/4Pb7ubYa4OrY1MH1sH17zPU8qFbJW4uFh4+Xl8ZcKM3dRN2u00mjb3RSuLb10ZOHb9+avqXlQIAbhOC7UwzWmCCffP/d1ZXCz1PNwd7Zbh3cucGxISxsvfnx9ufVmYYvN/bXDQXMcdiyR+/dh/kwD4Cwz0nwBkDB//1eMq8hYvtiFS7E5euvi4uajVt8MV699R/pZbyMFzH+BUVhy2E=:3253\r\n" + 
           		"\r\n" + 
           		"\r\n" + 
           		"\r\n" + 
           		"^FO670,130^GB100,30,2^FS\r\n" + 
           		"^FO680,137^A0N,25,25^FR^FD1 DE 4^FS\r\n" + 
           		"\r\n" + 
           		"^FT237,80^A0N,36,36^FDPizza Americana SAS^FS\r\n" + 
           		"^FO30,380^GB740,0,8^FS\r\n" + 
           		"\r\n" + 
           		"^FT360,120^A0N,28,28^FD# 789123^FS\r\n" + 
           		"\r\n" + 
           		"^FO400,255^GB300,40,20,B,5^FS\r\n" + 
           		"^FT680,120^BQN,2,3\r\n" + 
           		"^FH^FDLA,www.pizzaamericana.co/^FS\r\n" + 
           		"\r\n" + 
           		"^FO150,420^FB230,2,0,J^A0N,20,20^FDCLI: Pablo Diego José María de los\r\n" + 
           		"Remedios Picasso^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,460^FB350,3,0,J^A0N,20,20^FDDIR:San Antonio de Prado Carrera 68 #\r\n" + 
           		"70sur-40 Poblado: Carrera 30 Trans 7A -381 local 107^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,520^FB350,1,0,J^A0N,20,20^FDTEL: 3003001234^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,540^FB350,1,0,J^A0N,20,20^FDCOMP^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,560^FB360,4,0,J^A0N,17,17^FDOBS: The typesetting origin of the\r\n" + 
           		"field is fixed with respect to the contents of the field and does not\r\n" + 
           		"change with rotation..^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,140^A0N,16,16^FDPizza Americana Itagui^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,156^A0N,16,16^FDCr 50 A # 33-61 Local 107^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,172^A0N,16,16^FDHas tu pedido en linea www.pizzaamericana.co^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,188^A0N,16,16^FDtel: 4444553^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,204^A0N,16,16^FDPIZZA AMERICANA SAS^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,220^A0N,16,16^FD9012907451^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,236^A0N,16,16^FDREGIMÉN COMÚN^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,252^A0N,16,16^FDResolución # 18762015195196^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,268^A0N,16,16^FD2019-06-18^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,284^A0N,16,16^FDDesde P7 1 Hasta P7 100000^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,300^A0N,16,16^FDItagüí-Colombia^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,316^A0N,16,16^FDFactura de Venta:  123456^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,332^A0N,20,20^FDFecha/Hora: 02/04/2021 12:05:32^FS\r\n" + 
           		"\r\n" + 
           		"^FO30,427^A0N,28,28^FD# 456789^FS\r\n" + 
           		"\r\n" + 
           		"^FO410,140^FB230,2,0,J^A0N,20,20^FDCLI: Pablo Diego José María de los\r\n" + 
           		"Remedios Picasso^FS\r\n" + 
           		"\r\n" + 
           		"^FO410,180^FB350,1,0,J^A0N,20,20^FDTEL: 3003001234^FS\r\n" + 
           		"\r\n" + 
           		"^FO410,200^A0N,30,30^FDSubtotal: 14500.0^FS\r\n" + 
           		"\r\n" + 
           		"^FO410,230^A0N,30,30^FDINC 8%: 0.0^FS\r\n" + 
           		"\r\n" + 
           		"^FO410,260^A0N,40,40^FR^FDTotal: $145000.0^FS\r\n" + 
           		"\r\n" + 
           		"^FO410,300^A0N,30,30^FDCambio: 0^FS\r\n" + 
           		"\r\n" + 
           		"^FO410,330^A0N,30,30^FDEFECTIVO : 14.500^FS\r\n" + 
           		"\r\n" + 
           		"^FO300,360^A0N,20,20^FDDel horno a tus manos^FS\r\n" + 
           		"\r\n" + 
           		"^FO400,420^FB370,2,0,J^A0N,30,30^FDPeq Pepperoni champiñón / Jamón con\r\n" + 
           		"champiñón^FS\r\n" + 
           		"\r\n" + 
           		"^FO400,480^FB370,1,0,J^A0N,30,30^FDGAS:7Up^FS\r\n" + 
           		"\r\n" + 
           		"^FO400,510^FB370,3,0,J^A0N,17,17^FDADI: Doble queso, Jamón ahumado,\r\n" + 
           		"Salami, Pepperoni, Pollo, Chorizo de ternera, Tocineta, Tocinitos, Piña,\r\n" + 
           		"Champiñón, Tomate fresco^FS\r\n" + 
           		"\r\n" + 
           		"^FO400,561^FB370,3,0,J^A0N,17,17^FDMod: Doble queso, Jamón ahumado,\r\n" + 
           		"Salami, Pepperoni, Pollo, Chorizo de ternera, Tocineta, Tocinitos, Piña,\r\n" + 
           		"Champiñón, Tomate fresco^FS\r\n" + 
           		"\r\n" + 
           		"^PQ1,0,1,Y\r\n" + 
           		"^XZ";
           //String s = "^XA\\n\\r^MNM\\n\\r^FO050,50\\n\\r^B8N,100,Y,N\\n\\r^FD1234567\\n\\r^FS\\n\\r^PQ3\\n\\r^XZ";
                   

           //byte[] by = s.getBytes();
           
           
           DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
           Doc doc = new SimpleDoc(s.getBytes(), flavor, null);
          // System.out.println( by );
           job.print(doc, null);
           
       } catch (PrintException e) {
           e.printStackTrace();
       }      
   }
}