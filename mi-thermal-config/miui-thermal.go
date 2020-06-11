package main

import (
	"strings"
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"os"
	"fmt"
	"io/ioutil"
	"path"
	"errors"
	"flag"
)

var input string
var output string
var decryptMode bool
// AES-128。key长度：16, 24, 32 bytes 对应 AES-128, AES-192, AES-256
var key = []byte("thermalopenssl.h")

func main() {
	flag.StringVar(&input, "i", "/vendor/etc", "input dir or file path")
	flag.StringVar(&output, "o", "./outputs", "output dir out file path")
	flag.BoolVar(&decryptMode, "d", true, "output dir out file path")

	flag.Parse()

	fmt.Println("-i	", input)
	fmt.Println("-o	", output)
	fmt.Printf("-d	%t\n", decryptMode)
	fmt.Println("\n########################################\n")

	// testAes("thermal.conf")
	// decryptAll("./files")

	run()

	fmt.Println("\n########################################\n")
	fmt.Println("[ --- Application by dudusky --- ]")
}

func run() error {
	rd, err := ioutil.ReadDir(input) // ioutil.ReadDir("/vendor/etc")
	index := 0
	for _, fi := range rd {
		fileName := fi.Name()
		if ! fi.IsDir() && path.Ext(fileName) == ".conf" && strings.Index(fileName, "thermal") == 0 {
			index ++
			fmt.Printf("(%d) %s \n", index , input+"/" + fileName)

			if (decryptMode) {
				decryptThermal(input + "/" + fileName, fileName)
			} else {
				encryptThermal(input + "/" + fileName, fileName)
			}
		}
	}
	return err
}

func decryptThermal(filePath, fileName string) {
	os.MkdirAll(output, 0644)

	bytes, fileErr := ioutil.ReadFile(filePath)
	if fileErr != nil {
		fmt.Println("read fail", fileErr)
	} else {
		r, e := AesDecrypt(bytes, key)
		if e == nil {
			// fmt.Println(string(r))
			ioutil.WriteFile(output + "/" + fileName, r, 0644)
		} else {
			fmt.Println("Decryption failed", e)
		}
	}
}

func encryptThermal(filePath, fileName string) {
	os.MkdirAll(output, 0644)

	bytes, fileErr := ioutil.ReadFile(filePath)
	if fileErr != nil {
		fmt.Println("read fail", fileErr)
	} else {
		r, e := AesEncrypt(bytes, key)
		if e == nil {
			// fmt.Println(string(r))
			ioutil.WriteFile(output + "/" + fileName, r, 0644)
		} else {
			fmt.Println("Encryption failed", e)
		}
	}
}

func AesEncrypt(origData, key []byte) ([]byte, error) {
    block, err := aes.NewCipher(key)
    if err != nil {
			return nil, err
    }

    blockSize := block.BlockSize()
    origData = PKCS5Padding(origData, blockSize)
    // origData = ZeroPadding(origData, block.BlockSize())
    blockMode := cipher.NewCBCEncrypter(block, key[:blockSize])
    crypted := make([]byte, len(origData))
    blockMode.CryptBlocks(crypted, origData)
    return crypted, nil
}

func AesDecrypt(crypted, key []byte) ([]byte, error) {
    block, err := aes.NewCipher(key)
    if err != nil {
        return nil, err
    }
    blockSize := block.BlockSize()
    blockMode := cipher.NewCBCDecrypter(block, key[:blockSize])
		origData := make([]byte, len(crypted))

		/*
		// 错误拦截必须配合defer使用，通过匿名函数使用，在错误之前引用
    defer func() {
			err := recover()
			if err != nil {
				fmt.Println(">>>>>>>>>>>>>>>>>")
				fmt.Println(err)
			}
		}()
		*/

    if len(origData) % blockSize != 0 {
			return nil, errors.New("Unencrypted file，skipped!")
		}

    blockMode.CryptBlocks(origData, crypted)
    origData = PKCS5UnPadding(origData)
    // origData = ZeroUnPadding(origData)
    return origData, nil
}

func ZeroPadding(ciphertext []byte, blockSize int) []byte {
    padding := blockSize - len(ciphertext)%blockSize
    padtext := bytes.Repeat([]byte{0}, padding)
    return append(ciphertext, padtext...)
}

func ZeroUnPadding(origData []byte) []byte {
    length := len(origData)
    unpadding := int(origData[length-1])
    return origData[:(length - unpadding)]
}

func PKCS5Padding(ciphertext []byte, blockSize int) []byte {
    padding := blockSize - len(ciphertext)%blockSize
    padtext := bytes.Repeat([]byte{byte(padding)}, padding)
    return append(ciphertext, padtext...)
}

func PKCS5UnPadding(origData []byte) []byte {
    length := len(origData)
    // 去掉最后一个字节 unpadding 次
    unpadding := int(origData[length-1])
    return origData[:(length - unpadding)]
}