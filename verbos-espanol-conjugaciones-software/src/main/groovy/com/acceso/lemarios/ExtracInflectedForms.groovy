package com.acceso.lemarios

import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import org.jsoup.select.Elements

class ExtractInflectedForms {

	private final static String OUTPUT_FILE = '/tmp/inflectedForms.txt'
	private final static String VERB_FILE = '/com/acceso/lemarios/verbos-espanol.txt'
	private final static String LEMA_SEARCH_URL = 'http://lema.rae.es/drae/srv/search?val='
	private final static String INFLECTED_FORMS_URL = 'http://lema.rae.es/drae/srv/'
	private final static List STOP_WORDS = ['me', 'te', 'se', 'nos', 'os', 'etc.'] 

	private File outputFile = new File(OUTPUT_FILE)

	public void execute() {
		List<String> inflected = []
		List<String> verbs = loadVerbs()
		verbs.eachWithIndex { verb, index ->
			println "[${index}, ${verbs.size()}]"
			inflected.addAll(extract(verb))
		}

		//inflected = inflected.sort().unique()
		inflected.removeAll(STOP_WORDS)

		save(inflected)
	}

	private List<String> extract(String verb) {
		List<String> results = []
		List<String> inflectedUrls = getInflectedUrls(verb)

		//println "[${verb}] (${inflectedUrls.size()})"
		//inflectedUrls.each { println "\t${it}" }

		results = inflectedUrls.collect { 
			try {
				return getInflectedForms(verb, it)
			} catch(Exception e) {
				println "Error [${verb}] [${it}]: ${e}"
				return []
			}
		}.flatten()

		//println "Declinaciones " + results.join('--')

		return results
	}

	private List<String> getInflectedForms(String verb, String inflectedUrl) {
		Document doc = Jsoup.connect(inflectedUrl).get()
		Elements elements = doc.select("p[class=z]")

		List<String> results = elements.collect { element ->
			element.text().split(',|/| o | u | ').collect{it.trim()}.findAll{it}
		}.flatten()

		if(!results) {
			println "[0 inflected forms for ${verb}]"
		}

		return results
	}

	// Devuelve las urls con las declinaciones
	private List<String> getInflectedUrls(String verb) {
		List<String> result = []

		try {
			Document doc = Jsoup.connect(LEMA_SEARCH_URL + URLEncoder.encode(verb, 'ISO-8859-1')).get()
			Elements elements = doc.select("a img[alt^=Ver conjugaci]")

			result = elements.collect { element -> INFLECTED_FORMS_URL + element.parent().attr('href') }
		} catch (Exception e) {
			println "Error [${verb}] [${it}]: ${e}"
		}

		return result
	}

	private void save(List<String> forms) {
		forms.each { outputFile << "${it}\n" }
	}

	private List<String> loadVerbs() {
		List<String> result = []
		InputStream i = getClass().getResourceAsStream(VERB_FILE)

		if(i) {
			i.eachLine { line -> if(line.contains('í')) result << line }
		}

		return result
	}

	public static void main(String[] args) {
		ExtractInflectedForms e = new ExtractInflectedForms()
		e.execute()
	}
}
