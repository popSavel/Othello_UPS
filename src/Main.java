class Main
{
	public static void main(String [] args) throws Exception
	{
		SocketManager soc;
		if(args.length == 2){
			int port = Integer.parseInt(args[1]);
			soc = new SocketManager(args[0], port);
		}else if(args.length == 1){
			soc = new SocketManager(args[0]);
		}else{
			soc = new SocketManager();
		}
		soc.start();
		LoginWindow lgn = new LoginWindow(soc);
	}
}