/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.util;

import java.lang.reflect.Method;

/**
 * Utility for getting/setting java bean style properties on an object.
 * 
 * @author sgb
 */
public class PropertyUtil
{

    private static final Class<?>[] STRING_ARGS = {String.class};
    private static final Class<?>[] INT_ARGS = {int.class};
    
    public static void set(String propertyName, Object value, Object subject) 
    {
        Method m = getSetter(propertyName, subject, value);
        try
        {
            m.setAccessible(true);
            m.invoke(subject, value);
        } catch (Exception e)
        {
            throw new IllegalStateException("Could not set property:" + propertyName + " subject:" + subject + " new value:" + value,  e);
        }
    }
    
    /**
     * You don't want to clear the variable first unless you are setting some variable where the setting method is actually adding things to a list rather than overwriting.
     */
    public static void set(String propertyName, Object value, Object subject, boolean clearFirst) 
    {
    	if (clearFirst)
    	{
        	Method c = getClearer(propertyName, subject);
            try
            {
                c.setAccessible(true);
                c.invoke(subject);
            } catch (Exception e)
            {
                throw new IllegalStateException("Could not clear property:" + propertyName + " subject:" + subject + " new value:" + value,  e);
            }
    	}
    	set(propertyName, value, subject);
    }
    
    /**
     * You don't want to clear the variable unless you are setting some variable where the setting method is actually adding things to a list rather than overwriting.
     */
    public static void clear(String propertyName, Object subject)
    {
    	try
    	{
        	Method c = getClearer(propertyName, subject);
            c.setAccessible(true);
            c.invoke(subject);
    	}
        catch(Exception e)
        {
            throw new IllegalStateException("Could not clear property:" + propertyName + " subject:" + subject,  e);
        }
    }

    public static Object get(String propertyName, Object subject) 
    {
        try
        {
          Method getter = subject.getClass().getMethod("get" + capitalizeFirstLetter(propertyName), new Class[0]);
          return getter.invoke(subject, new Object[0]);
        }
        catch(Exception e)
        {
            throw new IllegalStateException("Could not get property:" + propertyName + " subject:" + subject,  e);
        }
    }
    public static Object getRaw(String property, Object subject) 
    {
        try
        {
          Method getter = subject.getClass().getMethod("getRawProperty", String.class);
          return getter.invoke(subject, property);
        }
        catch(Exception e)
        {
            throw new IllegalStateException("Could not get property:" + property + " subject:" + subject,  e);
        }
    }

    private static String capitalizeFirstLetter(String aString)
    {
      char first = aString.charAt(0);
      first = Character.toUpperCase(first);
      return first + aString.substring(1);
    }

    //TODO for some reason, territoryAttachments come into here as Integers in the History panel
    private static Method getSetter(String propertyName, Object subject, Object value) {
        String setterName = "set" + capitalizeFirstLetter(propertyName );
        for(Method m : subject.getClass().getDeclaredMethods()) {
            if(m.getName().equals(setterName)) 
            {
                try
                {
                    Class<?> argType = value.getClass();
                    return subject.getClass().getMethod(setterName, argType);
                } catch(NoSuchMethodException nsmf)
                {
                    //Go ahead and try the first one
                    return m;                    
                }
                    catch(NullPointerException n)
                {
                    //Go ahead and try the first one
                    return m;                    
                }   
            }
        }

        throw new IllegalStateException("No method called:" + setterName + " on:" + subject);
    }
    
    private static Method getClearer(String propertyName, Object subject) {
        String clearerName = "clear" + capitalizeFirstLetter(propertyName );
        for(Method c : subject.getClass().getDeclaredMethods()) {
            if(c.getName().equals(clearerName)) 
            {
                try
                {
                    return subject.getClass().getMethod(clearerName);
                } catch(NoSuchMethodException nsmf)
                {
                    //Go ahead and try the first one
                    return c;                    
                }
                    catch(NullPointerException n)
                {
                    //Go ahead and try the first one
                    return c;                    
                }   
            }
        }

        throw new IllegalStateException("No method called:" + clearerName + " on:" + subject);
    }
}
