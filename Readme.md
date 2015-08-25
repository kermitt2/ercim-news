Some fun with the ERCIM News collection and Grobid.

Installation of [Grobid](https://github.com/kermitt2/grobid) is necessary and the corresponding resource paths must be updated in the project property file (`ercim_news.properties`). 

Relevant links:

- [ERCIM News](http://ercim-news.ercim.eu), the magazine of ERCIM

- [ERCIM](http://www.ercim.eu), the European Research Consortium for Informatics and Mathematics

- [Grobid](https://github.com/kermitt2/grobid), a machine learning library for extracting information from Scientific Publications

- [Inria](http://www.inria.fr), French Institute for Research in Computer Science and Automation

## License

This code is distributed under [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). 

Main author and contact: Patrice Lopez (<patrice.lopez@inria.fr>)

The ERCIM News PDF issue present in this project is included only for test purposes and is copyright ERCIM.
In ERCIM News, all authors, as identified in each article, retain copyright of their work.

## Build

#### GROBID

[GROBID](http://github.com/kermitt2/grobid) must be installed for running the ERCIM tool. 

See the [GROBID documentation](http://grobid.readthedocs.org).

Depending on your installation path, set the path to your GROBID home in the project property file (```ercim_news.properties```). 

#### Build the tool

```bash
> mvn clean install
```

## Usage

Required input:

* PDF of the ERCIM news issue (example: ```resources/input/pdf/EN100-web.pdf```),

* catalogue information in TEI (example: ```resources/input/tei/ercim100.xml```).

The extraction process can be launched by the following command:

```bash
> mvn exec:exec -Pprocess_ercim 
```

The files for an importation in HAL Research Archive are generated under ```resources/hal/**volume**/```. Other generated files can be found ```resources/output/**volume**/```
