<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>

      <p:identity use-when="true()">
        <p:input port="source">
          <p:inline><success/></p:inline>
        </p:input>
      </p:identity>

      <p:identity use-when="false()">
        <p:input port="source">
          <p:inline><failure/></p:inline>
        </p:input>
      </p:identity>

    </p:declare-step>