package ru.rc.shml;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

public class ENTRY 
{
	public static void main(String[] args)
	{
		System.out.println("BEGIN");
		
		try 
		{
			//fix classloader issue with barebones jar
			//still need to download asm jar yourself
			Method m = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
			m.setAccessible(true);
			m.invoke(ClassLoader.getSystemClassLoader(), new File("spacehaven.jar").toURI().toURL());
			m.invoke(ClassLoader.getSystemClassLoader(), new File("asm-all-5.2.jar").toURI().toURL());
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		for (URL u : ((URLClassLoader) (ClassLoader.getSystemClassLoader())).getURLs())
		{
			System.out.println(u);
		}
		
		ClassLoader ucl = null;
		try 
		{
			ucl = (ClassLoader) Class.forName("ru.rc.shml.UnsafeURLClassLoader").newInstance();
		} 
		catch (Exception e1) 
		{
			//won't happen
		}
		try 
		{
			Object[] o = new Object[1];
			String[] ss = new String[1];
			ss[0] = "contentEditor";
			o[0] = ss;
			ucl.loadClass("fi.bugbyte.spacehaven.MainClass").getDeclaredMethods()[0].invoke(null, o);
			
			int i =0;
			for (Method m : ucl.loadClass("fi.bugbyte.spacehaven.MainClass").getDeclaredMethods())
			{
				System.out.println(i++ + " " + m);
			}
			
		} 
		catch (Exception e)
		{
			//can happen
			e.printStackTrace();
		}
	}
}