<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:px="http://xproc.dev.java.net/ns/extensions" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">

    <p:parameters>
      <p:input port="parameters">
        <p:inline>
          <c:param-set not-allowed="whatever">
            <c:param name="foo" namespace="http://bar.com" value="baz"/>
          </c:param-set>
        </p:inline>
      </p:input>
    </p:parameters>

  </p:declare-step>