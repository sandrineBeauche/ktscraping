package org.sbm4j.ktscraping.requests

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import org.sbm4j.ktscraping.core.RequestSender
import org.sbm4j.ktscraping.dowloaders.PlaywrightRequest


class GoogleSearchRequest(
    override val sender: RequestSender,
    val searchText: String,
    nbResults: Int
): PlaywrightRequest(sender,"http://www.google.fr", {
        val locAccepter = this.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Tout accepter"))
        if(locAccepter.count() > 0){
            locAccepter.click()
        }
        this.getByLabel("Rech.").click()
        this.getByLabel("Rech.").fill(searchText)
        this.getByLabel("Recherche Google").first().click()
        this.getByRole(AriaRole.LINK, Page.GetByRoleOptions().setName("Images").setExact(true)).click()
        this.locator("div#search").waitFor()
}) {

    init {
        parameters["cssSelectorImages"] = mapOf("div[data-docid] g-img" to nbResults)
    }
}