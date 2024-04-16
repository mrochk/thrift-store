all: build

build: clean
	@javac -d build src/*.java

run: build
	@java -cp build Main

clean:
	@rm -rf build
