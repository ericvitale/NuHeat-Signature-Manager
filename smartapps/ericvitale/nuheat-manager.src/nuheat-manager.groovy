/**
 *  NuHeat Manager
 *
 *  Copyright 2016 ericvitale@gmail.com
 *
 *  Version 1.0.0 - Initial Release
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *  You can find this smart app @ https://github.com/ericvitale/
 *  You can find my other device handlers & SmartApps @ https://github.com/ericvitale
 *
 */

include 'asynchttp_v1'

import groovy.time.TimeCategory
 
public static String version() { return "v0.0.001.20170415a" }
    /*
     * 12/17/2017 >>> v0.0.001.20171217a - Initial build.
     */
 
definition(
    name: "NuHeat Manager",
    namespace: "ericvitale",
    author: "Eric Vitale",
    description: "...",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    iconX3Url: "ttps://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
    page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
    	
        section("Account Settings") {
            input "nuheatUser", "text", title: "Username", description: "Email", required: true
            input "nuheatPassword", "text", title: "Password", description: "Password", required: true
            input "showPasswordInLogs", "bool", title: "Show Password in Log", required: true, defaultValue: false
	    }
    
	    section([mobileOnly:true], "Options") {
			label(title: "Assign a name", required: false)
            input "active", "bool", title: "Rules Active?", required: true, defaultValue: true
            input "logging", "enum", title: "Log Level", required: true, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
    	}
	}
}

/*************************************************************************************/

/**Begin Setup Methods****************************************************************/

def installed() {
    log("Begin installed.", "DEBUG")
    initialize()
    log("End installed.", "DEBUG")
}

def updated() {
    log("Begin updated().", "DEBUG")
    
    initialize()
    log("End updated().", "DEBUG")
}

def initialize() {
    log("Begin initialize().", "DEBUG")
    
    log("Username = ${nuheatUser}.", "INFO")
    log("Show Password in Logs = ${showPasswordInLogs}.", "INFO")
    
    if(showPasswordInLogs) {
    	log("Password = ${nuheatPassword}.", "INFO")
    }
    
    setUsername(nuheatUser)
    setPassword(nuheatPassword)
    setShowPassword(showPasswordInLogs)
    
    authenticateUser()
    
    subscribe(app, appHandler)
       
    log("End initialization().", "DEBUG")
}

/**End Setup Methods******************************************************************/

/**Begin Event Handlers**************************************************************************/

def appHandler(evt) {
    getT()
}

/**End Event Handlers****************************************************************************/

/**Begin Input Getters / Setters***********************************************************/

private setUsername(value) {
	state.username = value
}

private getUsername() {
	if(state.username == null) {
    	log("Username is NULL.", "ERROR")
    } else {
    	return state.username
   	}
}

private setPassword(value) {
	state.password = value
}

private getPassword() {
	if(state.password == null) {
    	log("Password is NULL.", "ERROR")
    } else {
    	return state.password
   	}	
}

private setShowPassword(value) {
	state.showThePassword = value
}

private getShowPassword() {
	if(state.showThePassword == null) {
    	state.showThePassword = false
    }
    
    return state.showThePassword
}

/**End Input Getters / Setters*************************************************************/


/**Begin Authroization Methods*************************************************************/
def authenticateUser() {

	log("Attempting to authenticate user ${getUsername()}.", "INFO")
    
    setSessionID("")
    
    def body = ["Email": "${getUsername()}", "password": "${getPassword()}", "application": "0"]
    
    def result = nuheatPost("https://www.mynuheat.com/api/authenticate/user", body)
    
    log("Result = ${result}", "DEBUG")
    
    log("SessionID = ${result["SessionId"]}", "INFO")
    
    setSessionID(result["SessionId"])

	if(getSessionID() != "") {
	    userAuthenticated(true)
        log("User has been authenticated.", "INFO")
        return true
    } else {
    	userAuthenticated(false)
        log("User failed to authenticate.", "ERROR")
        return false
    }
}

def isUserAuthenticated() {
	if(state.authenticatedUser == null) {
    	return false
    } else {
		return state.authenticatedUser
    }
}

def userAuthenticated(value) {
	state.authenticatedUser = value
}

private setSessionID(value) {
	state.sessionID = value
}

private getSessionID() {
	if(state.sessionID == null) {
    	state.sessionID = ""
    }
    return state.sessionID
}

/**End Authorization Methods***************************************************************/

/**Begin GET & POST Methods**********************************************************************/
def getT() {
    //; charset=utf-8
    def params = [
        uri: "https://www.mynuheat.com",
		path: "/api/thermostats?sessionid=${getSessionID()}",
        headers: ["Content-Type": "application/json", "Accept": "application/json", "Connection" : "close"]
    ]
    
    asynchttp_v1.get('getResponseHandler', params)
}

def getResponseHandler(response, data) {

	log("RESPONSE = ${response.getStatus()}.", "DEBUG")
    log("DATA = ${data}.", "DEBUG")

    if(response.getStatus() == 200 || response.getStatus() == 207) {
		log("Response received from NuHeat in the getReponseHandler.", "DEBUG")
        
        log("Response ${response.getJson()}", "DEBUG")
    } else {

    }
}

/**End GET & POST Methods************************************************************************/

/************ Begin Logging Methods *******************************************************/

def logPrefix() {
	return "NuHeatManager"
}

private determineLogLevel(data) {
    switch (data?.toUpperCase()) {
        case "TRACE":
            return 0
            break
        case "DEBUG":
            return 1
            break
        case "INFO":
            return 2
            break
        case "WARN":
            return 3
            break
        case "ERROR":
        	return 4
            break
        default:
            return 1
    }
}

def log(data, type) {
    data = "${logPrefix()} -- ${data ?: ''}"
        
    if (determineLogLevel(type) >= determineLogLevel(settings?.logging ?: "INFO")) {
        switch (type?.toUpperCase()) {
            case "TRACE":
                log.trace "${data}"
                break
            case "DEBUG":
                log.debug "${data}"
                break
            case "INFO":
                log.info "${data}"
                break
            case "WARN":
                log.warn "${data}"
                break
            case "ERROR":
                log.error "${data}"
                break
            default:
                log.error "NuHeatSig -- Invalid Log Setting"
        }
    }
}

/************ End Logging Methods *********************************************************/

/**Begin GET & POST Methods**********************************************************************/

private nuheatGet(url, value_map) {    
	log("Sending a GET to NuHeat.", "DEBUG")
    
    log("URL = ${url}", "INFO")
   
    def params = [uri: url, body: value_map]
    
    try {
		httpGet(params) { resp ->
			log("Response: ${resp}.", "TRACE")

            resp.headers.each {
               log("header ${it.name} : ${it.value}", "TRACE")
            }

            log("response contentType: ${resp.contentType}", "TRACE")
            log("response data: ${resp.data}", "TRACE")
            
            return resp.data
        }
    } catch (groovyx.net.http.HttpResponseException e) {
    	log("User is not authenticated, authenticating.", "ERROR")
        log("Exception: ${e.getMessage()}", "ERROR")
        
        if(e.getMessage() == "Unauthorized") {
        	authenticateUser()
        }
        
        return []
    }
}

private nuheatPost(url, value_map) {

	log("Sending a POST to NuHeat.", "DEBUG")
   
    def params = [uri: url, body: value_map]
    
    try {
		httpPost(params) { resp ->
			log("Response: ${resp}.", "TRACE")

            resp.headers.each {
               log("header ${it.name} : ${it.value}", "TRACE")
            }

            log("response contentType: ${resp.contentType}", "TRACE")
            log("response data: ${resp.data}", "TRACE")
            
            return resp.data
        }
    } catch (groovyx.net.http.HttpResponseException e) {
    	log("User is not authenticated, authenticating.", "ERROR")
        log("Exception: ${e.getMessage()}", "ERROR")
        
        if(e.getMessage() == "Unauthorized") {
        	authenticateUser()
        }
        
        return []
    }
}

/**End GET & POST Methods************************************************************************/