package org.cytoscape.webservice.psicquic;




public class PSICQUICUniversalClient {
//
//	private static final long serialVersionUID = 4485772700287524894L;
//
//	private static final String[] IMPORT_MODE = { "Detection Method", "Interaction ID" };
//	private static final String[] IMPORT_DATA_FORMAT = { "MITAB25", "PSI-MI XML 2.5" };
//	private static final String[] SEARCH_MODE = { "Interactor ID(s)", "Query", "Interaction ID(s)", "Pipeline" };
//
//	private enum Mode {
//		SEARCH, IMPORT;
//	}
//
//	private enum QueryMode {
//		GET_BY_INTERACTOR, GET_BY_QUERY;
//	}
//
//	private QueryMode qMode = QueryMode.GET_BY_INTERACTOR;
//
//	private void setProperty() {
//		props = new ModulePropertiesImpl(clientID, "wsc");
//
//		// General setting
//		props.add(new Tunable("block_size", "Block Size", Tunable.INTEGER, Integer.valueOf(100)));
//		props.add(new Tunable("timeout", "Timeout (sec.)", Tunable.INTEGER, new Integer(6000)));
//
//		final String[] modeArray = { QueryMode.GET_BY_INTERACTOR.name(), QueryMode.GET_BY_QUERY.toString() };
//		props.add(new Tunable("query_mode", "Query Mode", Tunable.LIST, Integer.valueOf(0), (Object) modeArray,
//				(Object) null, 0));
//
//	}
//
//
//	// Display name of this client.
//	private static final String DISPLAY_NAME = "PSICQUIC Universal Web Service Client";
//
//	// Client ID. This should be unique.
//	private static final String CLIENT_ID = "psicquic";
//
//	// // Instance of this client. This is a singleton.
//	private PSICQUICRestClient client;
//
//	// Visual Style name for the networks generated by this client.
//	private static final String DEF_VS_NAME = "PSI-MI 25 Style";
//	private VisualStyle defaultVS = null;
//
//	private Map<URI, QueryResponse> sResult;
//	private List<DbRef> queryList;
//	private String query;
//
//	private final ServiceState states;
//
//	static {
//		try {
//			client = new PSICQUICUniversalClient();
//		} catch (Exception e) {
//			CyLogger.getLogger().error("Could not initialize PSICQUIC Client.", e);
//		}
//	}
//
//	private PSICQUICUniversalClient() throws Exception {
//		super(CLIENT_ID, DISPLAY_NAME, new ClientType[] { ClientType.NETWORK }, null, new PSICQUICServiceRegistory(),
//				null);
//
//		setDescription();
//		// Set properties for this client.
//		setProperty();
//
//		this.states = new ServiceState(((PSICQUICServiceRegistory) clientStub));
//
//	}
//
//	public static WebServiceClient<PSICQUICServiceRegistory> getClient() {
//		return client;
//	}
//
//	public VisualStyle getDefaultVisualStyle() {
//		return PSI25VisualStyleBuilder.getDefVS();
//	}
//
//	@Override
//	public void executeService(CyWebServiceEvent e) throws CyWebServiceException {
//
//		if (e.getSource().equals(CLIENT_ID)) {
//			if (e.getEventType().equals(WSEventType.IMPORT_NETWORK)) {
//				SearchResultDialog searchResultDialog = new SearchResultDialog(Cytoscape.getDesktop(), states);
//				searchResultDialog.setVisible(true);
//				Set<String> selected = searchResultDialog.getSelected();
//
//				importNetwork(selected, null);
//			} else if (e.getEventType().equals(WSEventType.EXPAND_NETWORK)) {
//				SearchResultDialog searchResultDialog = new SearchResultDialog(Cytoscape.getDesktop(), states);
//				searchResultDialog.setVisible(true);
//				Set<String> selected = searchResultDialog.getSelected();
//				importNetwork(selected, Cytoscape.getCurrentNetwork());
//			} else if (e.getEventType().equals(WSEventType.SEARCH_DATABASE)) {
//				search(e.getParameter().toString(), e);
//			}
//		}
//
//	}
//
//	private void search(String queryString, CyWebServiceEvent<?> e) throws CyWebServiceException {
//
//		final PSICQUICServiceRegistory searchClient = ((PSICQUICServiceRegistory) clientStub);
//		sResult = null;
//		int blockSize = 100;
//		try {
//			blockSize = Integer.parseInt(props.getValue("block_size"));
//		} catch (Exception exp) {
//			blockSize = 100;
//		}
//
//		this.query = queryString;
//		final RequestInfo info = new RequestInfo();
//		info.setResultType(PSICQUICReturnType.COUNT.getTypeName());
//		info.setBlockSize(blockSize);
//		try {
//			Tunable mode = props.get("query_mode");
//			System.out.println("QueryMode ====> " + mode.getValue());
//
//			if (mode.getValue().equals(QueryMode.GET_BY_INTERACTOR.ordinal())) {
//				System.out.println("QueryMode ====> " + mode.getValue());
//				queryList = buildInteractorList(query);
//				sResult = searchClient.getCount(queryList, info, "OR");
//			} else {
//				// Get by Query (MIQL)
//				sResult = searchClient.getCount(query, info);
//			}
//
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			throw new CyWebServiceException(CyWebServiceException.WSErrorCode.REMOTE_EXEC_FAILED);
//		}
//
//		if (sResult == null)
//			return;
//
//		WSEventType nextMove = e.getNextMove();
//		if (nextMove == null)
//			nextMove = WSEventType.IMPORT_NETWORK;
//
//		Integer total = 0;
//		for (URI key : sResult.keySet()) {
//			final Integer count = sResult.get(key).getResultInfo().getTotalResults();
//			states.setRecentResultCount(states.getName(key.toString()), count);
//			total = total + sResult.get(key).getResultInfo().getTotalResults();
//		}
//
//		Cytoscape.firePropertyChange(SEARCH_FINISHED.toString(), this.clientID,
//				new DatabaseSearchResult<Map<URI, QueryResponse>>(total, sResult, nextMove));
//
//	}
//
//	private void importNetwork(Set<String> selected, CyNetwork e) throws CyWebServiceException {
//
//		if (selected == null || selected.size() == 0)
//			return;
//
//		PSICQUICServiceRegistory importClient = ((PSICQUICServiceRegistory) clientStub);
//		for (URI uri : importClient.getServiceNames().keySet()) {
//			String name = importClient.getServiceNames().get(uri);
//			if (selected.contains(name) == false)
//				importClient.skip.add(uri);
//		}
//
//		Map<URI, List<QueryResponse>> importResult = null;
//		final RequestInfo info = new RequestInfo();
//
//		info.setResultType(PSICQUICReturnType.MITAB25.getTypeName());
//		info.setBlockSize(100);
//		try {
//			if (queryList != null) {
//				// Use OR as operand
//				importResult = importClient.getByInteractorList(queryList, info, "OR");
//			} else {
//				importResult = importClient.getByQuery(query, info);
//			}
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			throw new CyWebServiceException(CyWebServiceException.WSErrorCode.REMOTE_EXEC_FAILED);
//		}
//
//		if (importResult == null)
//			return;
//
//		// Ask user to display names
//		final List<String> defNetworkNames = new ArrayList<String>();
//		final Date time = new Date(System.currentTimeMillis());
//		final Map<URI, String> nameMap = importClient.getServiceNames();
//		Map<URI, String> newNameMap = new HashMap<URI, String>();
//		String netName = null;
//		for (URI name : importResult.keySet()) {
//			if (importClient.isEmpty(name))
//				continue;
//			else {
//				netName = nameMap.get(name);
//				defNetworkNames.add(netName);
//				newNameMap.put(name, netName);
//			}
//		}
//
//		final ResultDialog report = new ResultDialog(Cytoscape.getDesktop(), true, newNameMap);
//		report.setLocationRelativeTo(Cytoscape.getDesktop());
//		report.setVisible(true);
//		report.setModal(true);
//
//		final Mitab25Mapper mapper = new Mitab25Mapper();
//		List<CyNetwork> target = new ArrayList<CyNetwork>();
//
//		// Create parent empty network
//		String parentName = "PSICQUIC Query Results";
//		String postfix = ": " + time.toString();
//		if (query != null)
//			postfix = postfix + " (" + query + ")";
//
//		parentName = parentName + postfix;
//
//		newNameMap = report.getNewNames();
//
//		final CyNetwork parentNetwork = Cytoscape.createNetwork(parentName, false);
//
//		for (URI key : importResult.keySet()) {
//
//			StringBuilder builder = new StringBuilder();
//			List<QueryResponse> res = importResult.get(key);
//			for (QueryResponse qr : res) {
//				builder.append(qr.getResultSet().getMitab());
//			}
//			final CyNetwork net = mapper.map(builder.toString(), newNameMap.get(key), parentNetwork);
//			if (net != null)
//				target.add(net);
//		}
//		final VisualMappingManager vmm = Cytoscape.getVisualMappingManager();
//		final VisualStyle defaultVS = PSI25VisualStyleBuilder.getDefVS();
//		if (vmm.getCalculatorCatalog().getVisualStyle(defaultVS.getName()) == null)
//			vmm.getCalculatorCatalog().addVisualStyle(defaultVS);
//
//		// Use nested network feature
//		final CyNode centerNode = Cytoscape.getCyNode(parentName, true);
//		parentNetwork.addNode(centerNode);
//
//		for (CyNetwork net : target) {
//			vmm.setVisualStyle(defaultVS);
//			final CyNetworkView targetView = Cytoscape.getNetworkView(net.getIdentifier());
//			targetView.setVisualStyle(defaultVS.getName());
//			targetView.redrawGraph(true, false);
//			CyNode nestedNode = Cytoscape.getCyNode(net.getTitle(), true);
//			nestedNode.setNestedNetwork(net);
//
//			Cytoscape.getNodeAttributes().setAttribute(nestedNode.getIdentifier(),
//					PSI25VisualStyleBuilder.ATTR_PREFIX + "interactor type", "nested");
//
//			parentNetwork.addNode(nestedNode);
//			parentNetwork.addEdge(Cytoscape.getCyEdge(nestedNode, centerNode, "interaction", "query_result", true));
//		}
//		Cytoscape.createNetworkView(parentNetwork);
//
//		query = null;
//		queryList = null;
//
//		// Call Advanced Network Merge
//		if (report.isMerge())
//			displayNetworkMerge();
//	}
//
//	/**
//	 * Use reflection to display the AdvancedNetworkMerge plugin.
//	 */
//	private void displayNetworkMerge() {
//		Class<?> advancedNetworkMergeClass;
//		try {
//			advancedNetworkMergeClass = Class.forName("csplugins.network.merge.NetworkMergePlugin");
//		} catch (ClassNotFoundException e1) {
//			CyLogger.getLogger().warn("Could not find Advanced Network Merge Plugin!", e1);
//			e1.printStackTrace();
//			return;
//		}
//
//		Method actionPerformedMethod;
//		try {
//			actionPerformedMethod = advancedNetworkMergeClass.getMethod("invokeAction");
//		} catch (SecurityException e2) {
//			e2.printStackTrace();
//			return;
//		} catch (NoSuchMethodException e2) {
//			e2.printStackTrace();
//			return;
//		}
//
//		try {
//			actionPerformedMethod.invoke(null);
//		} catch (IllegalArgumentException e3) {
//			e3.printStackTrace();
//			return;
//		} catch (IllegalAccessException e3) {
//			e3.printStackTrace();
//			return;
//		} catch (InvocationTargetException e3) {
//			e3.getCause().printStackTrace();
//			return;
//		}
//	}
//
//	// Build interactor list
//	private List<DbRef> buildInteractorList(String query) {
//		List<DbRef> interactorList = new ArrayList<DbRef>();
//
//		Pattern pattern2 = Pattern.compile(" +|\n|\t+");
//		String[] interactorNames = pattern2.split(query);
//
//		for (String name : interactorNames) {
//			System.out.println("==> " + name);
//			final DbRef dbRef = new DbRef();
//			dbRef.setId(name);
//			interactorList.add(dbRef);
//		}
//		return interactorList;
//	}
//
//
//	public TaskIterator createTaskIterator() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	public Object getSearchResult() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	public Set<CyNetwork> getNetworks() {
//		// TODO Auto-generated method stub
//		return null;
//	}

}