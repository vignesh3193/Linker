package linker_java;

import java.io.*;
import java.util.*;

class linker
{	
	static ArrayList<Integer> module_base_addresses=new ArrayList<Integer>();
	static Hashtable<String, Integer> isUsed=new Hashtable<>();

	public static void main(String args[])throws IOException
	{	


		String TERMINATOR_STRING = "end";
		System.out.println("ENTER INPUT, enter 'end' in a new line when finised");
		
		Scanner sc=new Scanner(System.in);
		StringBuilder b = new StringBuilder();
		String strLine;
		while (!(strLine = sc.nextLine()).equals(TERMINATOR_STRING)) {

			b.append("\n"+strLine);
		}

		sc.close();
		String file=b.toString();
		file=file+"\n0\n0\n0";

		Hashtable<String, Integer> symbol_table=new Hashtable<>();
		symbol_table=FirstPass(file);


		//System.out.println(module_base_addresses);

		String[] memory_map=new String[module_base_addresses.get(module_base_addresses.size()-1)];



		memory_map=SecondPass(file,memory_map.length,symbol_table);


		System.out.println("\nSymbol Table");
		System.out.println(symbol_table.toString());
		System.out.println();


		int count=0;
		System.out.println();
		System.out.println("Memory Map");
		
		for(String s: memory_map)
		{	
			System.out.println(count+":     "+s+" ");
			count++;
		}

		Enumeration e=isUsed.keys();
		System.out.println();


		//ERROR 3
		while(e.hasMoreElements())
		{
			String word=(String)e.nextElement();

			if(isUsed.get(word)==0)
			{
				System.out.println("warning "+ word+" Is defined but never used");
			}
		}


	}

	public static Hashtable<String, Integer> FirstPass(String file)
	{
		Hashtable<String, Integer> symbol_table1=new Hashtable<>();
		int module_number=0,linecount=0;
		ArrayList<String> definitions=new ArrayList<String>();

		module_base_addresses.add(0);




		Scanner sc = new Scanner(file);


		while(sc.hasNextLine())
		{
			String line="";

			int count=sc.nextInt();
			for(int j=0;j<count*2;j++)
			{	 
				line=line+sc.next()+" "; 
			}

			if(!line.isEmpty())
			{   
				line=line.trim();
				String[] line_array = line.split(" +");

				if(linecount==0 || linecount%3==0)
				{	
					//FIND WORD DEFINITION AND PUT IN SYMBOL TABLE OR DISPLAY ERROR IF DEFINED MULTIPLE TIMES
					for(int i=0;i<line_array.length;i=i+2)
					{	
						//ERROR 1
						if(symbol_table1.containsKey(line_array[i]))
						{
							System.out.println("ERROR "+line_array[i]+" defined multiple times, first value used");
						}
						else
						{
							symbol_table1.put(line_array[i], module_base_addresses.get(module_number)+Integer.parseInt(line_array[i+1]));

							definitions.add(line_array[i]);
							definitions.add(line_array[i+1]);
							isUsed.put(line_array[i],0);
						}
					}

				}

				//EVERY THIRD LINE USE PROGRAM TEXT TO CALCULATE AND STORE MODULE SIZE
				//ERROR 5

				if(linecount%3==2)
				{
					module_base_addresses.add(module_base_addresses.get(module_number)+count);

					module_number++;



					//TO CHECK IF DEFINITION EXCEEDS SIZE OF MODULE
					//ERROR 4

					for(int i=1;i<definitions.size();i=i+2)
					{	//System.out.println(definitions.toString());
						//System.out.println("errcheck"+definitions.get(i)+definitions.get(i-1)+" "+module_base_addresses.get(module_number) );
						//System.out.println("size"+(Integer.parseInt(definitions.get(i))+1));
						//System.out.println((module_base_addresses.get(module_number)-module_base_addresses.get(module_number-1)));
						//System.out.println(definitions.get(i-1));
						if((Integer.parseInt(definitions.get(i))+1)>(module_base_addresses.get(module_number)-module_base_addresses.get(module_number-1)))
						{

							System.out.println("ERROR, The value of "+definitions.get(i-1)+" is outside module size, 0 used(relative)");
							symbol_table1.put(definitions.get(i-1), symbol_table1.get(definitions.get(i-1))-Integer.parseInt(definitions.get(i)));

						}

					}definitions.clear();

				}


			}
			linecount++;
			if((linecount%3==0&&line.isEmpty())||(linecount%3==0&&line.equals("")))
			{
				definitions.clear();
			}


		}
		sc.close();



		return symbol_table1;
	}

	public static String[] SecondPass(String file, int length, Hashtable<String, Integer> symbol_table)
	{
		String[] mem_map=new String[length];
		int linecount=0;
		String[] module=new String[3];
		Integer external_address;
		int module_number=0;



		Scanner sc = new Scanner(file);
		int memcount=0;
		while(sc.hasNextLine())
		{	


			//AS SOON AS A MODULE GETS OVER, STORE ALL 3 LINES IN AN ARRAY TO USE
			if(linecount%3==0 && linecount!=0)
			{   

				module_number++;

				String[] use_list=module[1].split(" +");
				String[] program_text=module[2].split(" +");
				int[] chain_usage_list=new int[program_text.length/2];
				/*for(int i=0;i<chain_usage_list.length;i++)
					{
						chain_usage_list[i]=0;
					}*/


				//TRAVERSE USE LIST TO FIND START OF LINKED LIST AND BEGINNING SUBST EXT WORDS
				for(int i=0;i<use_list.length;i=i+2)
				{
					int current_index;

					if(symbol_table.containsKey(use_list[i])||use_list[i].equals(""))
					{	

						external_address=symbol_table.get(use_list[i]);
						isUsed.remove(use_list[i]);
						isUsed.put(use_list[i], 1);
					}
					else
					{	
						//ERROR 2
						System.out.println("ERROR "+use_list[i]+" not defined, 0 used");
						external_address=0;
					}
					if(i+1>=use_list.length)
					{
						current_index=777;
					}
					else
					{
						current_index=(Integer.parseInt(use_list[i+1])*2)+1;
						chain_usage_list[Integer.parseInt(use_list[i+1])]=1;
					}

					int next_index;

					while(current_index!=777)
					{	//System.out.println();
						//System.out.println("CI "+current_index);

						if(!program_text[current_index-1].equals("E"))
						{
							System.out.println("ERROR: "+program_text[current_index-1]+" type address on use chain; treated as E type.");

						}
						if(((Integer.parseInt(program_text[current_index])%1000)+1)>program_text.length&&((Integer.parseInt(program_text[current_index])%1000))!=777)
						{
							System.out.println("ERROR: Pointer in use chain exceeds module size; chain terminated.");
							chain_usage_list[(current_index-1)/2]=1;
							next_index=777;	
						}
						else if((Integer.parseInt(program_text[current_index])%1000)!=777)
						{	
							//keeping track of which words have been part of the use list linked list


							chain_usage_list[(current_index-1)/2]=1;
							//System.out.println(program_text[current_index]);
							next_index=(Integer.parseInt(program_text[current_index])%1000*2)+1;
						}
						else
						{
							chain_usage_list[(current_index-1)/2]=1;
							next_index=Integer.parseInt(program_text[current_index])%1000;
						}
						//System.out.println("NI "+next_index);

						int opcode=Integer.parseInt(program_text[current_index])/1000;
						opcode=opcode*1000;
						opcode=opcode+external_address;
						program_text[current_index]=Integer.toString(opcode);

						//for(String s: program_text)
						//System.out.print(s+"  ");

						current_index=next_index;
					}
				}

				for(int i=0;i<chain_usage_list.length;i++)
				{	
					if(chain_usage_list[i]==0)
					{	
						if(program_text[(i*2)].equals("E"))
						{
							System.out.println("ERROR: E type address not on use chain; treated as I type.");
						}
					}
				}

				//UPDATE PROGRAM TEXT WITH BASE ADDRESS OF MODULES TO GET EXACT ADDRESSES
				for(int i=0;i<program_text.length;i=i+2)
				{		
					int addr;
					if(program_text[i].equals("R"))
					{   
						addr=Integer.parseInt(program_text[i+1]);
						addr=addr+module_base_addresses.get(module_number-1);
						program_text[i+1]=Integer.toString(addr);

					}
				}

				//ADD TO FINAL MEMORY MAP ARRAY
				for(int j=1;j<program_text.length;j=j+2)
				{
					mem_map[memcount]=program_text[j];
					memcount++;
				}


			}
			String line="";

			//PARSE AND STORE EACH LINE OF CODE
			int count=sc.nextInt();
			for(int j=0;j<count*2;j++)
			{	 
				line=line+sc.next()+" "; 
			}

			//TO CHECK IF DEFINITION EXCEEDS SIZE OF MODULE
			/*	if(linecount%3==0)
				{
					String definitions[]=line.split(" +");
					if(!line.isEmpty())
					{	

						for(int i=0;i<definitions.length;i=i+2)
						{	
							if((Integer.parseInt(definitions[i+1])+1)>(module_base_addresses.get(module_number+1)-module_base_addresses.get(module_number)))
							{	
								System.out.println("ERROR, The value of "+definitions[i]+" is outisde module size, 0 used");
								symbol_table.put(definitions[i], symbol_table.get(definitions[i])-Integer.parseInt(definitions[i+1]));
							}
						}
					}
				}*/

			module[linecount%3]=line;
			//	System.out.println(module);

			linecount++;
		}

		sc.close();



		return mem_map;
	}

}