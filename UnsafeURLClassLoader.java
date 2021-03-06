package ru.rc.shml;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import sun.misc.Unsafe;

public class UnsafeURLClassLoader extends URLClassLoader
	{
		public Unsafe 	unsafe;
		public JarFile 	jar;
		
		public UnsafeURLClassLoader() throws MalformedURLException
		{
			//it use parent classloader directly
			//instead of using combination of proper classpath and super.
			super(new URL[0],null);
			try 
			{
				//works fine without unsafe actually
				//but unsafe still required to perform direct memory read\write
				Constructor<?> c = Class.forName("sun.misc.Unsafe").getDeclaredConstructors()[0];
				c.setAccessible(true);
				unsafe = (Unsafe) c.newInstance((Object[])null);
			} 
			catch (Exception e) 
			{
				//this won't happen
			}
			
			try 
			{
				jar = new JarFile("spacehaven.jar");
			} 
			catch (Exception e) 
			{
				//will happen if you run your jar outside of game's folder
				//or if your workplace do not have jar
			}
		}
		
		
	    public URL getResource(String name) 
	    {
	    	//this required by lwjgl loader
	        return ClassLoader.getSystemResource(name);
	    }
		
		public InputStream getResourceAsStream(String name) 
		{
			//this required by gdx loader
			JarEntry je = jar.getJarEntry(name);
			try 
			{
				return jar.getInputStream(je);
			} 
			catch (Exception e) 
			{
				//won't happen
			}
			return null;
		}
		
	    //this is JVM entry point, all calls land here
		//ever from native
	    public Class<?> loadClass(String name) throws ClassNotFoundException
	    {
			//System.out.println("loadClass=" + name);
			
			Class <?> RET = super.findLoadedClass(name);
			
			if (RET != null)
			{
				//System.out.println("already loaded");
				return RET;
			}
			
			//this is "bad" method but it works
			//sun is only required by lwjgl due to unsafe reference inside
			if (name.startsWith("java") || name.startsWith("sun"))
			{
				//System.out.println("system class");
				return ClassLoader.getSystemClassLoader().loadClass(name);
			}
			
			//workspace override part
			//must be kept in sync with your workspace config just in case
			//if your bin folder is not "bin"
			InputStream rawstream = null;
			File f = new File("bin/"+name.replace('.', '/').concat(".class"));
			
			if (f.exists())
			{
				System.out.println("workspace override " + name);
				try 
				{
					rawstream = new FileInputStream(f);
				} 
				catch (Exception e) 
				{
					//won't happen
				}
			}
			else
			{
				JarEntry je = jar.getJarEntry(name.replace('.', '/').concat(".class"));
				if (je == null)
					throw new ClassNotFoundException("no such class");
				try 
				{
					rawstream = jar.getInputStream(je);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
			
			try 
			{
				int size = rawstream.available();
				byte[] raw = new byte[size];

				int i = 0;
				
				while (i<size)
				{
					//this is uggly but this is exactly how it's done inside JDK classes
					//bundle read will fail for some classes due to nonstandard jar
					raw[i++] = (byte) rawstream.read();
				}
				
				raw = PatchHardcoded.walk(name, raw);
				
				return unsafe.defineClass(name, raw, 0, raw.length,this,null);
			} 
			catch (Exception e) 
			{
				//can happen
				e.printStackTrace();
			}
			return null;
	    }
	}