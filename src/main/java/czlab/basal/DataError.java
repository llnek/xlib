/**
 * Copyright © 2013-2020, Kenneth Leung. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package czlab.basal;

public class DataError extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   */
  public DataError(String m, Throwable t) {
    super(m, t);
  }

  /**
   */
  public DataError(Throwable t) {
    super(t);
  }

  /**
   */
  public DataError(String msg) {
    super(msg);
  }

}



