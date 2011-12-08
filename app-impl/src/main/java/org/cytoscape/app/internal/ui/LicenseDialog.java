package org.cytoscape.app.internal.ui;

//import org.cytoscape.plugin.internal.CytoscapePlugin;
import org.cytoscape.app.internal.DownloadableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class LicenseDialog extends javax.swing.JDialog {
	private static String title = "Plugin License Agreement";

	private static final Logger logger = LoggerFactory.getLogger(LicenseDialog.class);

    /** Creates new form LicenseDialog */
    public LicenseDialog() {
    	setModal(true);
		setTitle(title);
		setLocationRelativeTo(this);
        initComponents();
        listSetup();
    }

    public LicenseDialog(javax.swing.JDialog owner) {
		super(owner, title, true);
		setLocationRelativeTo(owner);
        initComponents();
        listSetup();
    }

    public LicenseDialog(javax.swing.JFrame owner) {
		super(owner, title, true);
		setLocationRelativeTo(owner);
        initComponents();
        listSetup();
    }

    private void listSetup() {
        listModel = new LicenseListModel();
        pluginList.setCellRenderer(new LicenseListCellRenderer());
        pluginList.setModel(listModel);
        pluginList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        acceptRadio.setEnabled(false);
        declineRadio.setEnabled(false);
    }
    
    public void addPlugin(DownloadableInfo obj) {
    	listModel.addElement(obj);
    }
    
    public void selectDefault() {
    	pluginList.setSelectedIndex(0);
  	DownloadableInfo infoObj = (DownloadableInfo) pluginList.getSelectedValue();
  	setLicenseText(infoObj.getLicenseText());
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">                          
    private void initComponents() {
        licenseSplitPane = new javax.swing.JSplitPane();
        pluginListScrollPane = new javax.swing.JScrollPane();
        pluginList = new javax.swing.JList();
        licenseScrollPane = new javax.swing.JScrollPane();
        licensePanel = new javax.swing.JEditorPane();
        buttonPane = new javax.swing.JPanel();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        acceptRadio = new javax.swing.JRadioButton();
        declineRadio = new javax.swing.JRadioButton();
        licenseLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        licenseSplitPane.setDividerLocation(150);
        pluginList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                pluginListValueChanged(evt);
            }
        });
        
        pluginListScrollPane.setViewportView(pluginList);

        licenseSplitPane.setLeftComponent(pluginListScrollPane);

        licenseScrollPane.setViewportView(licensePanel);

        licenseSplitPane.setRightComponent(licenseScrollPane);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        okButton.setText("Ok");
        okButton.setEnabled(false);
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        acceptRadio.setText("Accept All");
        acceptRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        acceptRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        acceptRadio.addActionListener( new java.awt.event.ActionListener () {
        	public void actionPerformed(java.awt.event.ActionEvent evt) {
        		radioAcceptEvent(evt);
        	}
        });
        		
        declineRadio.setText("Decline All");
        declineRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        declineRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        declineRadio.addActionListener( new java.awt.event.ActionListener() {
        	public void actionPerformed(java.awt.event.ActionEvent evt) {
        		radioDeclineEvent(evt);
        	}
        });

        org.jdesktop.layout.GroupLayout buttonPaneLayout = new org.jdesktop.layout.GroupLayout(buttonPane);
        buttonPane.setLayout(buttonPaneLayout);
        buttonPaneLayout.setHorizontalGroup(
            buttonPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, buttonPaneLayout.createSequentialGroup()
                .addContainerGap()
                .add(acceptRadio)
                .add(17, 17, 17)
                .add(declineRadio)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 221, Short.MAX_VALUE)
                .add(okButton)
                .add(20, 20, 20)
                .add(cancelButton)
                .addContainerGap())
        );
        buttonPaneLayout.setVerticalGroup(
            buttonPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(buttonPaneLayout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(buttonPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(acceptRadio)
                    .add(declineRadio)
                    .add(cancelButton)
                    .add(okButton)))
        );

        licenseLabel.setText("Cytoscape Plugin License Agreements");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, licenseLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 595, Short.MAX_VALUE)
                    .add(buttonPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(licenseSplitPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 595, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(licenseLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(14, 14, 14)
                .add(licenseSplitPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 356, Short.MAX_VALUE)
                .add(9, 9, 9)
                .add(buttonPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        pack();
    }// </editor-fold>                        

    private void radioAcceptEvent(java.awt.event.ActionEvent evt) {
    	acceptRadio.setSelected(true);
    	declineRadio.setSelected(false);
    	okButton.setEnabled(true);
    }

    private void radioDeclineEvent(java.awt.event.ActionEvent evt) {
    	declineRadio.setSelected(true);
    	acceptRadio.setSelected(false);
    	okButton.setEnabled(false);
    }
    
    // a listener has to be added in order to decide what to do after 'ok'
    public void addListenerToOk(java.awt.event.ActionListener listener) {
			okButton.addActionListener(listener);
    }

    
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {                                         
			logger.info("FINISHED");
			dispose(); 
    }                                        

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {                                             
    	this.dispose();
    }                                            

    private void pluginListValueChanged(javax.swing.event.ListSelectionEvent evt) {                                        
    	if (pluginList.getSelectedIndex() < 0) { // nothing selected, use the top license
    		pluginList.setSelectedIndex(0);
    	} 
    	DownloadableInfo infoObj = (DownloadableInfo) pluginList.getSelectedValue();
    	setLicenseText(infoObj.getLicenseText());

    	acceptRadio.setEnabled(true);
      declineRadio.setEnabled(true);
    }                                       
    
    private void setLicenseText(String licenseText) {
			licensePanel.setContentType("text/html");
			String Html = "<html><style type='text/css'>";
			Html += "body,th,td,div,p,h1,h2,li,dt,dd ";
			Html += "{ font-family: Tahoma, \"Gill Sans\", Arial, sans-serif; }";
			Html += "body { margin: 0px; color: #333333; background-color: #ffffff; }";
			Html += "#indent { padding-left: 30px; }";
			Html += "ul {list-style-type: none}";
			Html += "</style><body>";
			Html += licenseText;
			Html += "</body></html>";
			licensePanel.setText(Html);
			licensePanel.setEditable(false);
			licensePanel.setCaretPosition(0);
    }
    
    
    
    // Variables declaration - do not modify                     
    private javax.swing.JRadioButton acceptRadio;
    private javax.swing.JPanel buttonPane;
    private javax.swing.JButton cancelButton;
    private javax.swing.JRadioButton declineRadio;
    private javax.swing.JLabel licenseLabel;
    private javax.swing.JEditorPane licensePanel;
    private javax.swing.JScrollPane licenseScrollPane;
    private javax.swing.JSplitPane licenseSplitPane;
    private javax.swing.JButton okButton;
    private javax.swing.JList pluginList;
    private javax.swing.JScrollPane pluginListScrollPane;
    private LicenseListModel listModel;
    // End of variables declaration                   
    
    // class LicenseListModel
    class LicenseListModel extends javax.swing.AbstractListModel {
    	
    	private java.util.List<DownloadableInfo> licenseObjs;
    	public LicenseListModel() {
    		super();
    		licenseObjs = new java.util.ArrayList<DownloadableInfo>();
    	}
    	
    	public void addElement(DownloadableInfo obj) {
    			licenseObjs.add(obj);
    	}
    	
    	public void removeElement(int index) {
    		licenseObjs.remove(index);
    		fireContentsChanged(this, index, index);
    	}
    	
    	public int getSize() {
    		return licenseObjs.size();
    	}
    	
    	public Object getElementAt(int index) {
    		return licenseObjs.get(index);
    	}
    }
    
	// class LicenseListCellRenderer
	class LicenseListCellRenderer extends JLabel implements ListCellRenderer {
		public LicenseListCellRenderer() {
			setOpaque(true);
		}

		public Component getListCellRendererComponent(JList list, Object value, int index,
		                                              boolean isSelected, boolean cellHasFocus) {
			DownloadableInfo dInfo = (DownloadableInfo) value;
			setText(dInfo.getName());
			setToolTipText(dInfo.toString());
			setBackground(isSelected ? Color.gray : Color.white);
			setForeground(isSelected ? Color.white : Color.black);
			return this;
		}
	}
/*
	public static void main(String[] args) {
		LicenseDialog ld = new LicenseDialog();
		
		DownloadableInfo di_a = new cytoscape.plugin.PluginInfo();
		di_a.setName("Foobar");
		di_a.addCytoscapeVersion("2.6");
		di_a.setLicense("This is just some text", true);

		DownloadableInfo di_b = new cytoscape.plugin.PluginInfo();
		di_b.setName("Booya");
		di_b.addCytoscapeVersion("2.6");
		di_b.setLicense("Hi there!", true);

		
		ld.addPlugin(di_a);
		ld.addPlugin(di_b);
		ld.selectDefault();
		ld.setVisible(true);
	}
*/

}