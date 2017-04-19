/**
 * Copyright (c) 2013-2017, Kenneth Leung. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package czlab.basal;

import czlab.jasal.Gettable;
import czlab.jasal.Settable;
import java.util.Map;

/**
 * @author Kenneth Leung
 */
public interface Muble extends Gettable, Settable {

  /**/
  public Object getOrSet(Object k, Object v);

  /**
   * get list of attrributes
   */
  public Iterable<?> seq();

  /**
   * format to clojure EDN
   */
  public Object toEDN();

  /**
   * remove all attributes
   */
  public void clear();

  /**
   */
  public void copy(Muble from);

  /**
   */
  public void copyEx(Map<?,?> obj);

}

