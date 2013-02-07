package org.uhas.jmeter.plugin;

import java.beans.PropertyDescriptor;
import java.io.File;

import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.gui.TableEditor;
import org.apache.jmeter.testbeans.gui.TypeEditor;

public class NonGuiSummaryListenerBeanInfo extends BeanInfoSupport {

  public NonGuiSummaryListenerBeanInfo() {
    super( NonGuiSummaryListener.class );
    PropertyDescriptor p = null;

    createPropertyGroup( "basic", new String[]{ "file" , "testId"  } );
    p = property( "file" );
    p.setValue(DEFAULT, new File("summary-results.xml" ));
    p.setValue(NOT_UNDEFINED, Boolean.TRUE);

    p = property( "testId" );
    p.setValue(DEFAULT, "${__time(yyyyMMdd-HHmmss)}" );
    p.setValue(NOT_UNDEFINED, Boolean.TRUE);
    
    
    createPropertyGroup( "filter", new String[]{ "includedLabels" , "excludedLabels"  } );
    p = property( "includedLabels", TypeEditor.TableEditor );
    p.setValue( TableEditor.CLASSNAME, "java.lang.String" );
    p.setValue( TableEditor.HEADERS, new String[]{ "RegEx Pattern" } );
    
    p = property( "excludedLabels", TypeEditor.TableEditor );
    p.setValue( TableEditor.CLASSNAME, "java.lang.String" );
    p.setValue( TableEditor.HEADERS, new String[]{ "RegEx Pattern" } );
  }
  
  
  
}