# 🎬 LatestMoviesToPdf  

A Java-based project that fetches the latest movies using the **OMDB API** and generates a beautifully formatted **PDF report** using **iText Core 9**.  
The generated PDF includes movie details, posters, and a footer with a rating scale and signature.  

---

## ✨ Features  
- Fetches movie details dynamically from the **OMDB API**.  
- Generates a **PDF report** with:  
  - Movie posters & metadata  
  - Custom **rating scale table** (color-coded)  
  - Footer signature with project name & date  
- Built on **Java + iText Core 9** for PDF generation.  
- Configurable via external file (keeps your API key safe).  

---

## 🛠️ Tech Stack  
- **Java 8+**  
- **iText Core 9** (for PDF creation)  
- **OMDB API** (movie data)  

---

## 📦 Required JARs  

Make sure you add the following **iText Core 9 JARs** to your project’s `libs/` folder or Maven/Gradle dependencies:  

- `kernel-9.x.x.jar`  
- `layout-9.x.x.jar`  
- `io-9.x.x.jar`  
- `forms-9.x.x.jar`  
- `commons-9.x.x.jar`  

(Replace `9.x.x` with your installed version number)  

👉 You can download these from [iText Maven Repository](https://mvnrepository.com/artifact/com.itextpdf).  

If using **Maven**, add this snippet:  

```xml
<dependency>
  <groupId>com.itextpdf</groupId>
  <artifactId>kernel</artifactId>
  <version>9.0.0</version>
</dependency>
<dependency>
  <groupId>com.itextpdf</groupId>
  <artifactId>layout</artifactId>
  <version>9.0.0</version>
</dependency>
<dependency>
  <groupId>com.itextpdf</groupId>
  <artifactId>io</artifactId>
  <version>9.0.0</version>
</dependency>


⚙️ Setup
1. Clone the repo

    git clone https://github.com/your-username/latest-movies-to-pdf.git
    cd latest-movies-to-pdf


2. Add your OMDB API key

    Create a file named config.properties in the project root:
        OMDB_API_KEY=your_api_key_here

    (⚠️ This file is ignored in .gitignore for safety. Use config.example.properties as a reference.)

3. Build & Run

    javac -cp "libs/*" src/com/yourpackage/*.java
    java -cp "libs/*:src" com.yourpackage.LatestMoviesToPdf

🚀 Usage

>    Run the program.
>    Enter your desired movie name(s).
>    The program will fetch details & posters from OMDB.
>    PDF file (LatestMovies.pdf) will be generated in the project directory.

Example output:

>   🎥 Movie posters
>   📊 Rating scale table
>   ✍ Signature footer

📁 Repository Structure
    📦 latest-movies-to-pdf
    ┣ 📂 libs/                # iText JARs here
    ┣ 📂 src/com/yourpackage/ # Java source files
    ┣ 📜 config.example.properties # Sample API config
    ┣ 📜 .gitignore
    ┣ 📜 LICENSE
    ┣ 📜 README.md

🔑 Configuration

    OMDB API Key → Required. Get it from OMDB API.

    Store it in config.properties as shown above.

📜 License

This project is licensed under the MIT License – free to use, modify, and distribute with attribution.

🙌 Credits

    iText Core 9 – PDF generation
    OMDB API – Movie data source
