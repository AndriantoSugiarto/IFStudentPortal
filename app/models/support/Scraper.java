package models.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import id.ac.unpar.siamodels.Dosen;
import id.ac.unpar.siamodels.JadwalKuliah;
import id.ac.unpar.siamodels.Mahasiswa;
import id.ac.unpar.siamodels.Mahasiswa.Nilai;
import id.ac.unpar.siamodels.MataKuliah;
import id.ac.unpar.siamodels.MataKuliahFactory;
import id.ac.unpar.siamodels.Semester;
import id.ac.unpar.siamodels.TahunSemester;
import play.Play;

import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Scraper {
	private final String BASE_URL = "https://studentportal.unpar.ac.id/";
	private final String LOGIN_URL = BASE_URL + "C_home/sso_login";
	private final String SSO_URL = "https://sso.unpar.ac.id/login";
	private final String ALLJADWAL_URL = BASE_URL + "jadwal/seluruh_fakultas";
	private final String JADWAL_URL = BASE_URL + "jadwal";
	private final String NILAI_URL = BASE_URL + "nilai";
	private final String TOEFL_URL = BASE_URL + "nilai/toefl";
	private final String LOGOUT_URL = BASE_URL + "logout";
	private final String HOME_URL = BASE_URL + "home";
	private final String FRSPRS_URL = BASE_URL + "frs_prs";

	public void init() throws IOException {
		Connection baseConn = Jsoup.connect(BASE_URL);
		baseConn.timeout(0);
		baseConn.validateTLSCertificates(false);
		baseConn.method(Connection.Method.GET);
		baseConn.execute();
	}

	public String login(String npm, String pass) throws IOException {
		init();
		Mahasiswa logged_mhs = new Mahasiswa(npm);
		String user = logged_mhs.getEmailAddress();
		Connection conn = Jsoup.connect(LOGIN_URL);
		conn.data("Submit", "Login");
		conn.timeout(0);
		conn.validateTLSCertificates(false);
		conn.method(Connection.Method.POST);
		Response resp = conn.execute();
		Document doc = resp.parse();
		String lt = doc.select("input[name=lt]").val();
		String execution = doc.select("input[name=execution]").val();
		String jsessionid = resp.cookie("JSESSIONID");
		/* CAS LOGIN */
		Connection loginConn = Jsoup.connect(SSO_URL + ";jsessionid=" + jsessionid + "?service=" + LOGIN_URL);
		loginConn.cookies(resp.cookies());
		loginConn.data("username", user);
		loginConn.data("password", pass);
		loginConn.data("lt", lt);
		loginConn.data("execution", execution);
		loginConn.data("_eventId", "submit");
		loginConn.data("submit", "");
		loginConn.timeout(0);
		loginConn.validateTLSCertificates(false);
		loginConn.method(Connection.Method.POST);
		resp = loginConn.execute();
		if (resp.body().contains(user)) {
			Map<String, String> phpsessid = resp.cookies();
			return phpsessid.get("ci_session");
		} else {
			return null;
		}
	}

	public TahunSemester requestNamePhotoTahunSemester(String phpsessid, Mahasiswa mhs) throws IOException {
		Connection connection = Jsoup.connect(HOME_URL);
		connection.cookie("ci_session", phpsessid);
		connection.timeout(0);
		connection.validateTLSCertificates(false);
		connection.method(Connection.Method.GET);
		Response resp = connection.execute();
		Document doc = resp.parse();
		String nama = doc.select("div[class=namaUser d-none d-lg-block mr-3]").text();
		mhs.setNama(nama.substring(0, nama.indexOf(mhs.getEmailAddress())));
		Element photo = doc.select("img[class=img-fluid  fotoProfil]").first();
		String photoPathBase64 = photo.attr("src");
		String[] parsephoto = photoPathBase64.split(",");
		byte[] photoPathdecode = Base64.getDecoder().decode(parsephoto[1]);
		BufferedImage photoPath = ImageIO.read(new ByteArrayInputStream(photoPathdecode));
		String projectPath = Play.application().path().getAbsolutePath();
		File photoMhs = new File(projectPath + "\\public\\images\\" + mhs.getNpm() + ".jpeg");
		ImageIO.write(photoPath, "jpeg", photoMhs);
		mhs.setPhotoPath("/assets/images/"+mhs.getNpm()+".jpeg");
		connection = Jsoup.connect(FRSPRS_URL);
		connection.cookie("ci_session", phpsessid);
		connection.timeout(0);
		connection.validateTLSCertificates(false);
		connection.method(Connection.Method.GET);
		resp = connection.execute();
		doc = resp.parse();
		String curr_sem = doc.select(".custom-selectContent span").text();
		String[] sem_set = parseSemester(curr_sem);
		TahunSemester currTahunSemester = new TahunSemester(Integer.parseInt(sem_set[0]),
				Semester.fromString(sem_set[1]));
		return currTahunSemester;
	}

	public List<MataKuliah> requestAvailableKuliah(String phpsessid) throws IOException {
		//To Do Sementara untuk RequestAvailableKuliah diambil dari array daftar mata kuliah 2018
		MataKuliahFactory mkFactory = MataKuliahFactory.getInstance();
		String[] kodeMataKuliahKurikulum2018 = {
				"AIF181100","AIF181101","AIF181103","AIF181104","AIF181105","AIF181106","AIF181107",
				"AIF182007","AIF182100","AIF182101","AIF182103","AIF182105","AIF182106","AIF182109",
				"AIF182111","AIF182112","AIF182204","AIF182210","AIF182302","AIF182308","AIF183002",
				"AIF183010","AIF183013","AIF183015","AIF183106","AIF183107","AIF183111","AIF183112",
				"AIF183114","AIF183116","AIF183117","AIF183118","AIF183119","AIF183120","AIF183121",
				"AIF183122","AIF183123","AIF183124","AIF183128","AIF183141","AIF183143","AIF183145",
				"AIF183147","AIF183149","AIF183153","AIF183155","AIF183201","AIF183204","AIF183209",
				"AIF183225","AIF183227","AIF183229","AIF183232","AIF183236","AIF183238","AIF183250",
				"AIF183300","AIF183303","AIF183305","AIF183308","AIF183331","AIF183333","AIF183337",
				"AIF183339","AIF183340","AIF183342","AIF183346","AIF183348","AIF184000","AIF184001",
				"AIF184002","AIF184004","AIF184005","AIF184006","AIF184007","AIF184104","AIF184106",
				"AIF184108","AIF184109","AIF184110","AIF184114","AIF184115","AIF184116","AIF184119",
				"AIF184120","AIF184121","AIF184123","AIF184125","AIF184127","AIF184129","AIF184222",
				"AIF184224","AIF184228","AIF184230","AIF184231","AIF184232","AIF184233","AIF184235",
				"AIF184237","AIF184247","AIF184303","AIF184334","AIF184336","AIF184338","AIF184339",
				"AIF184340","AIF184341","AIF184342","AIF184343","AIF184344","AIF184345","MKU180110",
				"MKU180120","MKU180130","MKU180240","MKU180250","MKU180370","MKU180380"
		};
		List<MataKuliah> mkList;
		mkList = new ArrayList<>(kodeMataKuliahKurikulum2018.length);
		for (int i=0; i < kodeMataKuliahKurikulum2018.length; i++){
			MataKuliah curr = mkFactory.createMataKuliah(kodeMataKuliahKurikulum2018[i]);
			mkList.add(curr);
		}
		return mkList;
	}

	public List<JadwalKuliah> requestJadwal(String phpsessid) throws IOException {
		Connection connection = Jsoup.connect(JADWAL_URL);
		connection.cookie("ci_session", phpsessid);
		connection.timeout(0);
		connection.validateTLSCertificates(false);
		connection.method(Connection.Method.GET);
		Response resp = connection.execute();
		Document doc = resp.parse();
		Elements jadwalTable = doc.select("table[class=table table-responsive table-hover d-md-table ]");
		List<JadwalKuliah> jadwalList = new ArrayList<JadwalKuliah>();

		/* Kuliah */
		if (jadwalTable.size() > 0) {
			Elements tableKuliah = jadwalTable.get(0).select("tbody tr");
			String kode = new String();
			String nama = new String();
			for (Element elem : tableKuliah) {
				if (elem.className().contains("")) {
					if (!(elem.child(2).text().isEmpty() && elem.child(4).text().isEmpty())) {
						kode = elem.child(2).text();
						nama = elem.child(4).text();
					}
					MataKuliah currMk = MataKuliahFactory.getInstance().createMataKuliah(kode,
							Integer.parseInt(elem.child(5).text()), nama);
					try {
						String kelasString = elem.child(6).text();
						String hariString = elem.child(0).text();
						String waktuString = elem.child(1).text();
						if (hariString != null & hariString.length() != 0
								&& waktuString != null & waktuString.length() != 0) {
							jadwalList.add(
									new JadwalKuliah(currMk, kelasString.length() == 0 ? null : kelasString.charAt(0),
											new Dosen(null, elem.child(7).text()), hariString, waktuString,
											elem.child(3).text()));
						}
					} catch (IndexOutOfBoundsException e) {
						// void. do not add jadwal.
					}
				}
			}
		}
		return jadwalList;
	}

	public void requestNilai(String phpsessid, Mahasiswa logged_mhs) throws IOException, ScriptException {
		Connection connection = Jsoup.connect(NILAI_URL);
		connection.cookie("ci_session", phpsessid);
		connection.timeout(0);
		connection.validateTLSCertificates(false);
		connection.method(Connection.Method.POST);
		Response resp = connection.execute();
		Document doc = resp.parse();

		Elements dropdownSemester = doc.select("#dropdownSemester option");
		ArrayList<String> listSemester = new ArrayList<String>();
		for (Element semester : dropdownSemester){
			listSemester.add(semester.attr("value"));
		}
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("JavaScript");
		for (int l = 0; l < listSemester.size()-1; l++) {
			String[] thn_sem = listSemester.get(l).split("-");
			String thn = thn_sem[0];
			String sem = thn_sem[1];
			connection = Jsoup.connect(NILAI_URL + "/" + thn + "/" + sem);
			connection.cookie("ci_session", phpsessid);
			connection.timeout(0);
			connection.validateTLSCertificates(false);
			connection.method(Connection.Method.GET);
			resp = connection.execute();
			doc = resp.parse();

			Element script = doc.select("script").get(10);
			String scriptDataMataKuliah = script.html().substring(script.html().indexOf("var data_mata_kuliah = [];"), script.html().indexOf("var data_angket = [];"));
			engine.eval(scriptDataMataKuliah);
			ScriptObjectMirror dataMataKuliah = (ScriptObjectMirror) engine.get("data_mata_kuliah");
			TahunSemester tahunSemesterNilai = new TahunSemester(Integer.parseInt(thn), sem.charAt(0));
			for (Map.Entry<String, Object> mataKuliahEntry: dataMataKuliah.entrySet()){
				ScriptObjectMirror mataKuliah = (ScriptObjectMirror) mataKuliahEntry.getValue();
				MataKuliah curr_mk = MataKuliahFactory.getInstance().createMataKuliah((String) mataKuliah.get("kode_mata_kuliah"), Integer.parseInt((String) mataKuliah.get("jumlah_sks")), (String) mataKuliah.get("nama_mata_kuliah"));
				logged_mhs.getRiwayatNilai()
						.add(new Nilai(tahunSemesterNilai, curr_mk, (String) mataKuliah.get("na")));
			}
		}
	}

	public void requestNilaiTOEFL(String phpsessid, Mahasiswa mahasiswa) throws IOException {
		SortedMap<LocalDate, Integer> nilaiTerakhirTOEFL = new TreeMap<>();
		Connection connection = Jsoup.connect(TOEFL_URL);
		connection.cookie("ci_session", phpsessid);
		connection.timeout(0);
		connection.validateTLSCertificates(false);
		connection.method(Connection.Method.POST);
		Response resp = connection.execute();
		Document doc = resp.parse();
		Elements nilaiTOEFL = doc.select("table").select("tbody").select("tr");
		if (!nilaiTOEFL.isEmpty()) {
			for (int i = 0; i < nilaiTOEFL.size(); i++) {
				Element nilai = nilaiTOEFL.get(i).select("td").get(5);
				Element tgl_toefl = nilaiTOEFL.get(i).select("td").get(1);
				String[] tanggal = tgl_toefl.text().split(" ");
				switch (tanggal[1].toLowerCase()) {
				case "januari":
					tanggal[1] = "1";
					break;
				case "februari":
					tanggal[1] = "2";
					break;
				case "maret":
					tanggal[1] = "3";
					break;
				case "april":
					tanggal[1] = "4";
					break;
				case "mei":
					tanggal[1] = "5";
					break;
				case "juni":
					tanggal[1] = "6";
					break;
				case "juli":
					tanggal[1] = "7";
					break;
				case "agustus":
					tanggal[1] = "8";
					break;
				case "september":
					tanggal[1] = "9";
					break;
				case "oktober":
					tanggal[1] = "10";
					break;
				case "november":
					tanggal[1] = "11";
					break;
				case "desember":
					tanggal[1] = "12";
					break;
				}

				LocalDate localDate = LocalDate.of(Integer.parseInt(tanggal[2]), Integer.parseInt(tanggal[1]),
						Integer.parseInt(tanggal[0]));

				nilaiTerakhirTOEFL.put(localDate, Integer.parseInt(nilai.text()));
			}
		}
		mahasiswa.setNilaiTOEFL(nilaiTerakhirTOEFL);
	}

	public void logout() throws IOException {
		Connection logoutConn = Jsoup.connect(LOGOUT_URL);
		logoutConn.timeout(0);
		logoutConn.validateTLSCertificates(false);
		logoutConn.method(Connection.Method.GET);
		logoutConn.execute();
	}

	public String[] parseSemester(String sem_raw) {
		String[] sem_set = sem_raw.split("/")[0].split("-");
		return new String[] { sem_set[1].trim(), sem_set[0].trim() };
	}
}
