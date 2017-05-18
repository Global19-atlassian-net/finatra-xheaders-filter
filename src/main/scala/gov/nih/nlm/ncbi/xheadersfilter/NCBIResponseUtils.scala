package gov.nih.nlm.ncbi.xheadersfilter

import com.twitter.finagle.http.Response

/**
  * Created by anastasia on 5/12/17.
  */
object NCBIResponseUtils {

  def fixLocation(r: Response): Response = {
    r.headerMap.set(XHeadersHttpResponseFilter.XDisableScriptName, "True")
    r
  }

}
